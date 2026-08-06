package org.fentanylsolutions.thaumicdabblery.feature.researchscangates;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

public final class ScanGatePlayerHandler {

    static final ScanGatePlayerHandler INSTANCE = new ScanGatePlayerHandler();

    private ScanGatePlayerHandler() {}

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (ResearchScanGatesFeature.isEnabled() && event.player instanceof EntityPlayerMP) {
            ScanGateRegistry.reconcile((EntityPlayerMP) event.player);
        }
    }
}
