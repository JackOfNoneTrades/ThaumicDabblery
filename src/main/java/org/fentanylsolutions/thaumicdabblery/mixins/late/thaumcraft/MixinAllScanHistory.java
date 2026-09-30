package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import org.fentanylsolutions.thaumicdabblery.feature.scanall.AllScanHistory;
import org.fentanylsolutions.thaumicdabblery.feature.scanall.ScanAll;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.research.ResearchManager;

@Mixin(value = ResearchManager.class, remap = false)
public abstract class MixinAllScanHistory {

    @Inject(method = "completeScannedObjectUnsaved", at = @At("RETURN"))
    private static void td$allItems(String player, String key, CallbackInfoReturnable<Boolean> cir) {
        if (AllScanHistory.MARKER.equals(key)) ScanAll.wrap(Thaumcraft.proxy.getScannedObjects(), player);
    }

    @Inject(method = "completeScannedEntityUnsaved", at = @At("RETURN"))
    private static void td$allEntities(String player, String key, CallbackInfoReturnable<Boolean> cir) {
        if (AllScanHistory.MARKER.equals(key)) ScanAll.wrap(Thaumcraft.proxy.getScannedEntities(), player);
    }
}
