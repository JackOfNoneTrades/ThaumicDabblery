package org.fentanylsolutions.thaumicdabblery.feature.planarvortex;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

import com.kentington.thaumichorizons.common.tiles.TileVortex;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.wands.ItemWandCasting;

/** Small, player-specific previews, calculated on the server without sending the entire result queue. */
public final class VortexFeedback {

    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("td_vortex_hud");
    private final Map<EntityPlayerMP, NBTTagCompound> previous = new WeakHashMap<>();

    public static void initialize() {
        CHANNEL.registerMessage(Handler.class, Preview.class, 0, Side.CLIENT);
        FMLCommonHandler.instance()
            .bus()
            .register(new VortexFeedback());
    }

    public static void animate(TileVortex tile, long started) {
        Preview packet = new Preview();
        packet.data = new NBTTagCompound();
        packet.data.setLong("craftStarted", started);
        VortexCrafting.State state = VortexCrafting.state(tile);
        if (state.suction != null) {
            packet.data.setInteger("inputId", state.inputId);
            packet.data.setDouble("inputX", state.suction.fromX);
            packet.data.setDouble("inputY", state.suction.fromY);
            packet.data.setDouble("inputZ", state.suction.fromZ);
        }
        packet.data.setInteger("dimension", tile.getWorldObj().provider.dimensionId);
        packet.data.setInteger("x", tile.xCoord);
        packet.data.setInteger("y", tile.yCoord);
        packet.data.setInteger("z", tile.zCoord);
        CHANNEL.sendToAllAround(
            packet,
            new NetworkRegistry.TargetPoint(
                tile.getWorldObj().provider.dimensionId,
                tile.xCoord + .5,
                tile.yCoord + .5,
                tile.zCoord + .5,
                64));
    }

    public static NBTTagCompound preview(TileVortex vortex, ItemStack stack, EntityPlayerMP player) {
        NBTTagCompound data = new NBTTagCompound();
        if (!vortex.items.isEmpty() || VortexRecipes.pending(vortex)
            .isEmpty() || stack == null || !(stack.getItem() instanceof ItemWandCasting)) return data;
        NBTTagCompound base = VortexRecipes.pending(vortex)
            .get(0)
            .getCompoundTag("vis");
        NBTTagCompound costs = new NBTTagCompound(), missing = new NBTTagCompound();
        ItemWandCasting wand = (ItemWandCasting) stack.getItem();
        for (Object key : base.func_150296_c()) {
            String name = (String) key;
            Aspect aspect = Aspect.getAspect(name);
            int units = base.getInteger(name);
            if (aspect == null || !aspect.isPrimal() || units <= 0 || units > 100000) return data;
            AspectList single = new AspectList().add(aspect, units);
            int amount = Math.max(0, (int) (units * 100 * wand.getConsumptionModifier(stack, player, aspect, true)));
            // Respect native/API creative exemptions as well as ordinary crafting discounts.
            if (amount > 0) {
                ItemStack empty = stack.copy();
                wand.storeVis(empty, aspect, 0);
                if (wand.consumeAllVisCrafting(empty, player, single, false)) amount = 0;
            }
            costs.setInteger(name, amount);
            missing.setBoolean(name, !wand.consumeAllVisCrafting(stack, player, single, false));
        }
        if (costs.hasNoTags()) return data;
        data.setInteger("dimension", vortex.getWorldObj().provider.dimensionId);
        data.setInteger("x", vortex.xCoord);
        data.setInteger("y", vortex.yCoord);
        data.setInteger("z", vortex.zCoord);
        data.setTag("costs", costs);
        data.setTag("missing", missing);
        return data;
    }

    @SubscribeEvent
    public void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != Side.SERVER
            || !(event.player instanceof EntityPlayerMP)
            || event.player.ticksExisted % 5 != 0) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (player.playerNetServerHandler == null) return;
        NBTTagCompound data = new NBTTagCompound();
        ItemStack held = player.getHeldItem();
        if (held != null && held.getItem() instanceof ItemWandCasting) {
            double reach = Math.min(32, player.theItemInWorldManager.getBlockReachDistance());
            Vec3 eye = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
            Vec3 look = player.getLookVec();
            MovingObjectPosition hit = player.worldObj
                .rayTraceBlocks(eye, eye.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach));
            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                TileEntity tile = player.worldObj.getTileEntity(hit.blockX, hit.blockY, hit.blockZ);
                if (tile instanceof TileVortex) data = preview((TileVortex) tile, held, player);
            }
        }
        NBTTagCompound old = previous.put(player, data);
        if (!data.equals(old) || !data.hasNoTags() && player.ticksExisted % 20 == 0) {
            Preview packet = new Preview();
            packet.data = data;
            CHANNEL.sendTo(packet, player);
        }
    }

    public static final class Preview implements IMessage {

        public NBTTagCompound data;

        public void toBytes(ByteBuf buf) {
            ByteBufUtils.writeTag(buf, data);
        }

        public void fromBytes(ByteBuf buf) {
            data = ByteBufUtils.readTag(buf);
        }
    }

    public static final class Handler implements IMessageHandler<Preview, IMessage> {

        public IMessage onMessage(Preview packet, MessageContext context) {
            ThaumicDabblery.proxy.receiveVortexPreview(packet.data, context.netHandler);
            return null;
        }
    }
}
