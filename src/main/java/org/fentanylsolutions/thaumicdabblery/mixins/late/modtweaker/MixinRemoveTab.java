package org.fentanylsolutions.thaumicdabblery.mixins.late.modtweaker;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import modtweaker2.mods.thaumcraft.research.OrphanResearch;
import modtweaker2.mods.thaumcraft.research.RemoveTab;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;

@Mixin(value = RemoveTab.class, remap = false)
public abstract class MixinRemoveTab {

    @Shadow
    private String tab;

    @Unique
    private final List<OrphanResearch> thaumicdabblery$orphanActions = new ArrayList<>();

    @Inject(method = "apply", at = @At("HEAD"))
    private void thaumicdabblery$detachResearchReferences(CallbackInfo ci) {
        thaumicdabblery$orphanActions.clear();
        ResearchCategoryList category = ResearchCategories.getResearchList(tab);
        if (category == null) {
            return;
        }

        for (String researchKey : new ArrayList<>(category.research.keySet())) {
            OrphanResearch action = new OrphanResearch(researchKey);
            action.apply();
            thaumicdabblery$orphanActions.add(action);
        }
    }

    @Inject(method = "undo", at = @At("TAIL"))
    private void thaumicdabblery$restoreResearchReferences(CallbackInfo ci) {
        for (int index = thaumicdabblery$orphanActions.size() - 1; index >= 0; index--) {
            OrphanResearch action = thaumicdabblery$orphanActions.get(index);
            if (action.canUndo()) {
                action.undo();
            }
        }
    }
}
