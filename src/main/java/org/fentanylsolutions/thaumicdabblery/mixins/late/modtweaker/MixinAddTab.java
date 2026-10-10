package org.fentanylsolutions.thaumicdabblery.mixins.late.modtweaker;

import org.fentanylsolutions.thaumicdabblery.feature.researcheditor.ResearchEditor;
import org.fentanylsolutions.thaumicdabblery.feature.researcheditor.ResearchLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import modtweaker2.mods.thaumcraft.research.AddTab;
import thaumcraft.api.research.ResearchCategories;

@Mixin(value = AddTab.class, remap = false)
public abstract class MixinAddTab {

    @Shadow
    private String tab;
    @Unique
    private ResearchLayout thaumicdabblery$beforeReplacement;

    @Inject(method = "apply", at = @At("HEAD"))
    private void thaumicdabblery$replacePendingTab(CallbackInfo ci) {
        thaumicdabblery$beforeReplacement = ResearchEditor.prepareTabAddition(tab);
        if (thaumicdabblery$beforeReplacement != null) ResearchCategories.researchCategories.remove(tab);
    }

    @Inject(method = "undo", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$restoreReplacedTab(CallbackInfo ci) {
        if (thaumicdabblery$beforeReplacement != null) {
            // Remove the replacement even when the original tab did not exist.
            ResearchCategories.researchCategories.remove(tab);
            thaumicdabblery$beforeReplacement.apply();
            ci.cancel();
        }
    }
}
