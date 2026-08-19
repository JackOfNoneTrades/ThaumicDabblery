package org.fentanylsolutions.thaumicdabblery.mixins.late.modtweaker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import minetweaker.MineTweakerAPI;
import modtweaker2.mods.thaumcraft.research.SetResearch;
import thaumcraft.api.research.ResearchCategories;

@Mixin(value = SetResearch.class, remap = false)
public abstract class MixinSetResearch {

    @Unique
    private boolean thaumicdabblery$ignoredMissingResearch;

    @Shadow
    private String key;

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$rejectMissingResearch(CallbackInfo ci) {
        if (ResearchCategories.getResearch(key) == null) {
            thaumicdabblery$ignoredMissingResearch = true;
            MineTweakerAPI
                .logError("Cannot update flags for missing Thaumcraft research " + key + ". The action was ignored.");
            ci.cancel();
        }
    }

    @Inject(method = "canUndo", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$allowIgnoredActionCleanup(CallbackInfoReturnable<Boolean> cir) {
        if (thaumicdabblery$ignoredMissingResearch) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "undo", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$skipMissingResearch(CallbackInfo ci) {
        if (thaumicdabblery$ignoredMissingResearch) {
            ci.cancel();
            return;
        }

        if (ResearchCategories.getResearch(key) == null) {
            MineTweakerAPI.logWarning(
                "Could not restore flags for missing Thaumcraft research " + key + ". The undo was skipped.");
            ci.cancel();
        }
    }
}
