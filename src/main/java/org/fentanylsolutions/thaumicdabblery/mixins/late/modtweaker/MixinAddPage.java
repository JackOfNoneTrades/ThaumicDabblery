package org.fentanylsolutions.thaumicdabblery.mixins.late.modtweaker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import modtweaker2.mods.thaumcraft.research.AddPage;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;

@Mixin(value = AddPage.class, remap = false)
public abstract class MixinAddPage {

    @Shadow
    private String key;

    @Shadow
    private String tab;

    @Inject(method = "undo", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$skipUndoForMissingResearch(CallbackInfo ci) {
        ResearchCategoryList category = ResearchCategories.getResearchList(tab);
        if (category == null || category.research.get(key) == null) {
            ci.cancel();
        }
    }
}
