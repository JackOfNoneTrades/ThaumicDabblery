package org.fentanylsolutions.thaumicdabblery.mixins.late.modtweaker;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.ResearchPrerequisitesZen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import minetweaker.IUndoableAction;
import modtweaker2.mods.thaumcraft.research.AddPrereq;

@Mixin(value = AddPrereq.class, remap = false)
public abstract class MixinAddPrereq {

    @Unique
    private IUndoableAction thaumicdabblery$change;

    @Shadow
    private String key;

    @Shadow
    private String prereq;

    @Shadow
    private boolean hidden;

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$setPrerequisite(CallbackInfo ci) {
        thaumicdabblery$change = ResearchPrerequisitesZen.update(key, prereq, hidden);
        thaumicdabblery$change.apply();
        ci.cancel();
    }

    @Inject(method = "canUndo", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$allowActionCleanup(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    @Inject(method = "undo", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$restorePrerequisites(CallbackInfo ci) {
        if (thaumicdabblery$change != null) thaumicdabblery$change.undo();
        ci.cancel();
    }
}
