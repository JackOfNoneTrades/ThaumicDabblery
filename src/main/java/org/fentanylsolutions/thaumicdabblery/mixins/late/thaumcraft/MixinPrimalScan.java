package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;

import org.fentanylsolutions.thaumicdabblery.feature.customaspects.PrimalDiscovery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.research.ScanManager;

@Mixin(value = ScanManager.class, remap = false)
public abstract class MixinPrimalScan {

    @Inject(method = "checkAndSyncAspectKnowledge", at = @At("HEAD"), cancellable = true)
    private static void td$gateNotifications(EntityPlayer player, Aspect aspect, int amount,
        CallbackInfoReturnable<Integer> cir) {
        if (!PrimalDiscovery.allowed(Thaumcraft.proxy.getPlayerKnowledge(), player.getCommandSenderName(), aspect))
            cir.setReturnValue(0);
    }

    @Redirect(
        method = "completeScan",
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/common/lib/research/ScanManager;checkAndSyncAspectKnowledge(Lnet/minecraft/entity/player/EntityPlayer;Lthaumcraft/api/aspects/Aspect;I)I"))
    private static int td$itemDiscovery(EntityPlayer player, Aspect aspect, int amount, EntityPlayer scanner,
        ScanResult scan, String prefix) {
        return PrimalDiscovery.itemScan(scan)
            ? PrimalDiscovery.grant(
                player.getCommandSenderName(),
                aspect,
                () -> ScanManager.checkAndSyncAspectKnowledge(player, aspect, amount))
            : ScanManager.checkAndSyncAspectKnowledge(player, aspect, amount);
    }

    @Inject(method = "isValidScanTarget", at = @At("RETURN"), cancellable = true)
    private static void td$rescan(EntityPlayer player, ScanResult scan, String prefix,
        CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && ScanManager.hasBeenScanned(player, scan)
            && PrimalDiscovery.hasUnknownPrimal(player, scan)
            && ScanManager.validScan(ScanManager.getScanAspects(scan, player.worldObj), player))
            cir.setReturnValue(true);
    }

    @Inject(method = "completeScan", at = @At("RETURN"))
    private static void td$recovery(EntityPlayer player, ScanResult scan, String prefix,
        CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) PrimalDiscovery.revealScannedPrimals(player, scan, prefix);
    }
}
