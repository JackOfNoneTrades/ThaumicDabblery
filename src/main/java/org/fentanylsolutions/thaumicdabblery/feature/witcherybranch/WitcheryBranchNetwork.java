package org.fentanylsolutions.thaumicdabblery.feature.witcherybranch;

import net.minecraft.entity.player.EntityPlayerMP;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

public final class WitcheryBranchNetwork {

    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE
        .newSimpleChannel(ThaumicDabblery.MODID);

    private WitcheryBranchNetwork() {}

    static void initialize() {
        CHANNEL.registerMessage(UseMessage.Handler.class, UseMessage.class, 0, Side.SERVER);
        FMLCommonHandler.instance()
            .bus()
            .register(WitcheryBranchVirtualUse.INSTANCE);
        ThaumicDabblery.debug("[Mystic Branch/network] Registered virtual-use packet handler");
    }

    static void send(Action action) {
        ThaumicDabblery.debug("[Mystic Branch/network] Sending " + action + " to server");
        CHANNEL.sendToServer(new UseMessage(action));
    }

    enum Action {
        START,
        FINISH,
        CANCEL
    }

    public static final class UseMessage implements IMessage {

        private Action action;

        public UseMessage() {}

        private UseMessage(Action action) {
            this.action = action;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            int ordinal = buffer.readUnsignedByte();
            action = ordinal < Action.values().length ? Action.values()[ordinal] : Action.CANCEL;
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeByte(action.ordinal());
        }

        public static final class Handler implements IMessageHandler<UseMessage, IMessage> {

            @Override
            public IMessage onMessage(UseMessage message, MessageContext context) {
                EntityPlayerMP player = context.getServerHandler().playerEntity;
                ThaumicDabblery.debug(
                    "[Mystic Branch/network] Received " + message.action + " from " + player.getCommandSenderName());
                try {
                    switch (message.action) {
                        case START -> WitcheryBranchVirtualUse.begin(player);
                        case FINISH -> WitcheryBranchVirtualUse.finish(player);
                        case CANCEL -> WitcheryBranchVirtualUse.cancel(player);
                    }
                    ThaumicDabblery.debug(
                        "[Mystic Branch/network] Handled " + message.action + " for " + player.getCommandSenderName());
                } catch (RuntimeException | LinkageError error) {
                    ThaumicDabblery.LOG.error(
                        "Failed to handle virtual Mystic Branch action {} for {}",
                        message.action,
                        player.getGameProfile(),
                        error);
                }
                return null;
            }
        }
    }
}
