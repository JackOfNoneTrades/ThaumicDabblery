package org.fentanylsolutions.thaumicdabblery.feature.osmotic;

import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import thaumic.tinkerer.common.block.tile.TileEnchanter;
import thaumic.tinkerer.common.block.tile.container.ContainerEnchanter;

/** Validate selections and research on the server tick, including selections made before a script reload. */
public final class OsmoticPackets {

    public interface Request {

        int[] thaumicdabblery$request();
    }

    private static final ConcurrentLinkedQueue<Runnable> PENDING = new ConcurrentLinkedQueue<>();

    public static void clear() {
        PENDING.clear();
    }

    public static void initialize() {
        FMLCommonHandler.instance()
            .bus()
            .register(new OsmoticPackets());
    }

    public static void enqueue(Request request, MessageContext context) {
        if (!context.side.isServer()) return;
        EntityPlayerMP player = context.getServerHandler().playerEntity;
        int[] data = request.thaumicdabblery$request();
        PENDING.add(() -> {
            if (player.isDead || player.playerNetServerHandler == null
                || !player.worldObj.playerEntities.contains(player)
                || player.dimension != data[0]
                || !(player.openContainer instanceof ContainerEnchanter)
                || player.getDistanceSq(data[1] + .5, data[2] + .5, data[3] + .5) > 64
                || !player.worldObj.blockExists(data[1], data[2], data[3])) return;
            TileEntity tile = player.worldObj.getTileEntity(data[1], data[2], data[3]);
            if (!(tile instanceof TileEnchanter) || player.openContainer.getSlot(0).inventory != tile) return;
            if (data.length == 4) OsmoticRecipes.start((TileEnchanter) tile, player);
            else OsmoticRecipes.select((TileEnchanter) tile, player, data[4], data[5]);
        });
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Runnable task;
        while ((task = PENDING.poll()) != null) task.run();
    }
}
