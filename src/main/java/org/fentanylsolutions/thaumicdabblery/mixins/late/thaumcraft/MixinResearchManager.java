package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import org.fentanylsolutions.thaumicdabblery.feature.researchscangates.ResearchScanGatesFeature;
import org.fentanylsolutions.thaumicdabblery.feature.researchscangates.ScanGateRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import thaumcraft.common.lib.research.ResearchManager;

@Mixin(value = ResearchManager.class, remap = false)
public abstract class MixinResearchManager {

    @Inject(method = "doesPlayerHaveRequisites", at = @At("RETURN"), cancellable = true)
    private static void thaumicdabblery$requireScanGate(String playerName, String researchKey,
        CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && ResearchScanGatesFeature.isEnabled()
            && !ScanGateRegistry.hasRevealMarker(playerName, researchKey)) {
            cir.setReturnValue(false);
        }
    }
}
