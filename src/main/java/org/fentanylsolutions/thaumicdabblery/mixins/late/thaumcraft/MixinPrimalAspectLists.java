package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import org.fentanylsolutions.thaumicdabblery.feature.customaspects.CustomAspectRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

@Mixin(value = ThaumcraftApiHelper.class, remap = false)
public abstract class MixinPrimalAspectLists {

    @Inject(method = "getAllAspects", at = @At("HEAD"), cancellable = true)
    private static void td$all(int amount, CallbackInfoReturnable<AspectList> cir) {
        if (!CustomAspectRegistry.hasChanges()) return;
        AspectList result = new AspectList();
        for (Aspect aspect : Aspect.aspects.values()) result.add(aspect, amount);
        cir.setReturnValue(result);
    }

    @Inject(method = "getAllCompoundAspects", at = @At("HEAD"), cancellable = true)
    private static void td$compounds(int amount, CallbackInfoReturnable<AspectList> cir) {
        if (!CustomAspectRegistry.hasChanges()) return;
        AspectList result = new AspectList();
        for (Aspect aspect : Aspect.getCompoundAspects()) result.add(aspect, amount);
        cir.setReturnValue(result);
    }
}
