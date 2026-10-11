package org.fentanylsolutions.thaumicdabblery.feature.effigyskins;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

public final class EffigySkinNetwork {

    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("td_effigy");

    public static void initialize() {
        CHANNEL.registerMessage(Handler.class, Binding.class, 0, Side.CLIENT);
        CHANNEL.registerMessage(SparkleHandler.class, Sparkle.class, 1, Side.CLIENT);
        FMLCommonHandler.instance()
            .bus()
            .register(new EffigySkinNetwork());
    }

    public static void sync(EntityPlayerMP player) {
        if (player.playerNetServerHandler != null) CHANNEL.sendTo(Binding.from(player.getEntityData()), player);
    }

    public static void sparkle(net.minecraft.world.World world, int x, int y, int z, int color, int count) {
        Sparkle effect = new Sparkle();
        effect.dimension = world.provider.dimensionId;
        effect.x = x;
        effect.y = y;
        effect.z = z;
        effect.color = color;
        effect.count = count;
        CHANNEL
            .sendToAllAround(effect, new NetworkRegistry.TargetPoint(effect.dimension, x + 0.5, y + 0.5, z + 0.5, 32));
    }

    public static final class Sparkle implements IMessage {

        public int dimension, x, y, z, color, count;

        @Override
        public void fromBytes(ByteBuf buffer) {
            dimension = buffer.readInt();
            x = buffer.readInt();
            y = buffer.readInt();
            z = buffer.readInt();
            color = buffer.readInt();
            count = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(dimension);
            buffer.writeInt(x);
            buffer.writeInt(y);
            buffer.writeInt(z);
            buffer.writeInt(color);
            buffer.writeInt(count);
        }
    }

    public static final class SparkleHandler implements IMessageHandler<Sparkle, IMessage> {

        @Override
        public IMessage onMessage(Sparkle message, MessageContext context) {
            ThaumicDabblery.proxy.receiveEffigySparkle(message, context.netHandler);
            return null;
        }
    }

    @SubscribeEvent
    public void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) sync((EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP) sync((EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP) sync((EntityPlayerMP) event.player);
    }

    public static final class Binding implements IMessage {

        private boolean bound;
        private int dimension, x, y, z;

        public Binding() {}

        public static Binding from(NBTTagCompound data) {
            Binding result = new Binding();
            int[] coords = data.getIntArray("soulBeaconCoords");
            result.bound = data.getBoolean("soulBeacon") && coords.length == 3;
            if (result.bound) {
                result.dimension = data.getInteger("soulBeaconDim");
                result.x = coords[0];
                result.y = coords[1];
                result.z = coords[2];
            }
            return result;
        }

        public boolean matches(TileEntity vat) {
            return bound && vat.getWorldObj() != null
                && dimension == vat.getWorldObj().provider.dimensionId
                && x == vat.xCoord
                && y == vat.yCoord + 1
                && z == vat.zCoord;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            bound = buffer.readBoolean();
            if (bound) {
                dimension = buffer.readInt();
                x = buffer.readInt();
                y = buffer.readInt();
                z = buffer.readInt();
            }
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeBoolean(bound);
            if (bound) {
                buffer.writeInt(dimension);
                buffer.writeInt(x);
                buffer.writeInt(y);
                buffer.writeInt(z);
            }
        }
    }

    public static final class Handler implements IMessageHandler<Binding, IMessage> {

        @Override
        public IMessage onMessage(Binding message, MessageContext context) {
            ThaumicDabblery.proxy.receiveEffigyBinding(message, context.netHandler);
            return null;
        }
    }
}
