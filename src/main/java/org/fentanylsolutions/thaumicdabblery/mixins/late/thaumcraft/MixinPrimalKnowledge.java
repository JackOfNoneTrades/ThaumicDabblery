package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import org.fentanylsolutions.thaumicdabblery.feature.customaspects.CustomAspectRegistry;
import org.fentanylsolutions.thaumicdabblery.feature.customaspects.PrimalDiscovery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.research.PlayerKnowledge;

@Mixin(value = PlayerKnowledge.class, remap = false)
public abstract class MixinPrimalKnowledge {

    @Inject(method = "getAspectsDiscovered", at = @At("RETURN"))
    private void td$visiblePrimals(String player, CallbackInfoReturnable<AspectList> cir) {
        for (Aspect aspect : CustomAspectRegistry.visiblePrimals())
            if (!cir.getReturnValue().aspects.containsKey(aspect)) cir.getReturnValue()
                .add(aspect, 0);
    }

    @Inject(method = "addDiscoveredAspect", at = @At("HEAD"), cancellable = true)
    private void td$discoveryGate(String player, Aspect aspect, CallbackInfoReturnable<Boolean> cir) {
        if (!PrimalDiscovery.allowed((PlayerKnowledge) (Object) this, player, aspect)) cir.setReturnValue(false);
    }

    @Inject(method = { "addAspectPool", "setAspectPool" }, at = @At("HEAD"), cancellable = true)
    private void td$poolGate(String player, Aspect aspect, short amount, CallbackInfoReturnable<Boolean> cir) {
        if (!PrimalDiscovery.allowed((PlayerKnowledge) (Object) this, player, aspect)) cir.setReturnValue(false);
    }
}
