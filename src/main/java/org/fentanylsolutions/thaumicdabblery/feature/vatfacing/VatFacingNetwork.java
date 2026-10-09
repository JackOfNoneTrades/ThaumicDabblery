package org.fentanylsolutions.thaumicdabblery.feature.vatfacing;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

import com.kentington.thaumichorizons.common.tiles.TileVat;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

public final class VatFacingNetwork {

    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("td_vat_facing");

    public static void initialize() {
        CHANNEL.registerMessage(Handler.class, Pose.class, 0, Side.CLIENT);
        MinecraftForge.EVENT_BUS.register(new VatFacing());
        Item brain = GameRegistry.findItem("Thaumcraft", "ItemZombieBrain");
        if (brain != null) VatFacing.rotationItem = new ItemStack(brain);
    }

    public static void send(TileVat vat) {
        Pose pose = new Pose();
        pose.dimension = vat.getWorldObj().provider.dimensionId;
        pose.x = vat.xCoord;
        pose.y = vat.yCoord;
        pose.z = vat.zCoord;
        pose.data = new NBTTagCompound();
        VatFacing.state(vat)
            .write(pose.data);
        CHANNEL.sendToAllAround(
            pose,
            new NetworkRegistry.TargetPoint(pose.dimension, pose.x + 0.5, pose.y + 0.5, pose.z + 0.5, 128));
    }

    public static final class Pose implements IMessage {

        public int dimension, x, y, z;
        public NBTTagCompound data;

        public void toBytes(ByteBuf buf) {
            buf.writeInt(dimension);
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);
            ByteBufUtils.writeTag(buf, data);
        }

        public void fromBytes(ByteBuf buf) {
            dimension = buf.readInt();
            x = buf.readInt();
            y = buf.readInt();
            z = buf.readInt();
            data = ByteBufUtils.readTag(buf);
        }
    }

    public static final class Handler implements IMessageHandler<Pose, IMessage> {

        public IMessage onMessage(Pose pose, MessageContext context) {
            ThaumicDabblery.proxy.receiveVatFacing(pose, context.netHandler);
            return null;
        }
    }
}
