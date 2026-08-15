package org.fentanylsolutions.thaumicdabblery.mixins.late.modtweaker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import minetweaker.MineTweakerAPI;
import modtweaker2.mods.thaumcraft.research.MoveResearch;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;

@Mixin(value = MoveResearch.class, remap = false)
public abstract class MixinMoveResearch {

    @Shadow
    private String key;

    @Shadow
    private String newTab;

    @Shadow
    private int x;

    @Shadow
    private int y;

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$rejectInvalidDestination(CallbackInfo ci) {
        ResearchItem movingResearch = ResearchCategories.getResearch(key);
        if (movingResearch == null) {
            return;
        }

        ResearchCategoryList destination = ResearchCategories.getResearchList(newTab);
        if (destination == null) {
            MineTweakerAPI.logError(
                "Cannot move Thaumcraft research " + key
                    + " to unknown category "
                    + newTab
                    + ". The move was ignored.");
            ci.cancel();
            return;
        }
        if (movingResearch.isVirtual()) {
            return;
        }

        for (ResearchItem existing : destination.research.values()) {
            if (existing != movingResearch && existing.displayColumn == x && existing.displayRow == y) {
                MineTweakerAPI.logError(
                    "Cannot move Thaumcraft research " + key
                        + " to "
                        + newTab
                        + " ("
                        + x
                        + ", "
                        + y
                        + "): the position is occupied by "
                        + existing.key
                        + ". The move was ignored.");
                ci.cancel();
                return;
            }
        }
    }
}
