package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import org.fentanylsolutions.thaumicdabblery.feature.customaspects.PrimalDiscovery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.research.ResearchManager;

@Mixin(value = ResearchManager.class, remap = false)
public abstract class MixinPrimalResearch {

    @Inject(method = "completeAspectUnsaved", at = @At("HEAD"), cancellable = true)
    private static void td$initialGrant(String player, Aspect aspect, short amount,
        CallbackInfoReturnable<Boolean> cir) {
        if (!PrimalDiscovery.allowed(Thaumcraft.proxy.getPlayerKnowledge(), player, aspect)) cir.setReturnValue(false);
    }

    @Redirect(
        method = "loadAspectNBT",
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/common/lib/research/ResearchManager;completeAspectUnsaved(Ljava/lang/String;Lthaumcraft/api/aspects/Aspect;S)Z"))
    private static boolean td$restoreKnowledge(String player, Aspect aspect, short amount) {
        return PrimalDiscovery
            .grant(player, aspect, () -> ResearchManager.completeAspectUnsaved(player, aspect, amount));
    }
}
