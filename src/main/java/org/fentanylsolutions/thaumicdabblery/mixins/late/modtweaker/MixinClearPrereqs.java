package org.fentanylsolutions.thaumicdabblery.mixins.late.modtweaker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import minetweaker.MineTweakerAPI;
import modtweaker2.mods.thaumcraft.research.ClearPrereqs;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;

@Mixin(value = ClearPrereqs.class, remap = false)
public abstract class MixinClearPrereqs {

    @Unique
    private boolean thaumicdabblery$ignoredMissingResearch;

    @Unique
    private boolean thaumicdabblery$applied;

    @Shadow
    private String key;

    @Shadow
    private String[] prereqs;

    @Shadow
    private String[] secretPrereqs;

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$rejectMissingResearch(CallbackInfo ci) {
        if (ResearchCategories.getResearch(key) == null) {
            thaumicdabblery$ignoredMissingResearch = true;
            MineTweakerAPI.logError(
                "Cannot clear prerequisites for missing Thaumcraft research " + key + ". The action was ignored.");
            ci.cancel();
        } else {
            thaumicdabblery$applied = true;
        }
    }

    @Inject(method = "canUndo", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$allowIgnoredActionCleanup(CallbackInfoReturnable<Boolean> cir) {
        if (thaumicdabblery$ignoredMissingResearch || thaumicdabblery$applied) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "undo", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$restorePrerequisites(CallbackInfo ci) {
        if (thaumicdabblery$ignoredMissingResearch) {
            ci.cancel();
            return;
        }

        ResearchItem research = ResearchCategories.getResearch(key);
        if (research == null) {
            MineTweakerAPI.logWarning(
                "Could not restore prerequisites for missing Thaumcraft research " + key + ". The undo was skipped.");
        } else {
            research.parents = prereqs;
            research.parentsHidden = secretPrereqs;
        }
        ci.cancel();
    }
}
