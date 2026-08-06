package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;

import org.fentanylsolutions.thaumicdabblery.feature.researchscangates.ResearchScanGatesFeature;
import org.fentanylsolutions.thaumicdabblery.feature.researchscangates.ScanGateRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import thaumcraft.api.research.ScanResult;
import thaumcraft.common.lib.research.ScanManager;

@Mixin(value = ScanManager.class, remap = false)
public abstract class MixinScanManager {

    @Inject(method = "isValidScanTarget", at = @At("RETURN"), cancellable = true)
    private static void thaumicdabblery$allowRecoveryScan(EntityPlayer player, ScanResult scan, String prefix,
        CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && ResearchScanGatesFeature.isEnabled()
            && ScanManager.hasBeenScanned(player, scan)
            && ScanGateRegistry.hasIncompleteRequirement(player, scan)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "completeScan", at = @At("HEAD"), cancellable = true)
    private static void thaumicdabblery$completeRecoveryScan(EntityPlayer player, ScanResult scan, String prefix,
        CallbackInfoReturnable<Boolean> cir) {
        if (ResearchScanGatesFeature.isEnabled() && ScanManager.hasBeenScanned(player, scan)
            && ScanGateRegistry.hasActiveMatch(player, scan)) {
            ScanGateRegistry.recordScan(player, scan);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "completeScan", at = @At("RETURN"))
    private static void thaumicdabblery$completeNewScan(EntityPlayer player, ScanResult scan, String prefix,
        CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && ResearchScanGatesFeature.isEnabled()) {
            ScanGateRegistry.recordScan(player, scan);
        }
    }
}
