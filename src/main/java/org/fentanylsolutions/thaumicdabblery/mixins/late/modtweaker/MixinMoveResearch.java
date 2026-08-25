package org.fentanylsolutions.thaumicdabblery.mixins.late.modtweaker;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
            if (existing != movingResearch && !existing.isVirtual()
                && existing.displayColumn == x
                && existing.displayRow == y) {
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

    @Redirect(
        method = "moveResearchItem",
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/api/research/ResearchItem;registerResearchItem()Lthaumcraft/api/research/ResearchItem;"),
        require = 1)
    private ResearchItem thaumicdabblery$registerIgnoringVirtualOccupants(ResearchItem movingResearch) {
        if (movingResearch.isVirtual()) {
            return movingResearch.registerResearchItem();
        }

        ResearchCategoryList destination = ResearchCategories.getResearchList(movingResearch.category);
        if (destination == null) {
            return movingResearch.registerResearchItem();
        }

        List<ResearchItem> virtualOccupants = thaumicdabblery$removeVirtualOccupants(destination, movingResearch);
        try {
            return movingResearch.registerResearchItem();
        } finally {
            for (ResearchItem virtualResearch : virtualOccupants) {
                destination.research.put(virtualResearch.key, virtualResearch);
            }
        }
    }

    @Unique
    private static List<ResearchItem> thaumicdabblery$removeVirtualOccupants(ResearchCategoryList category,
        ResearchItem movingResearch) {
        List<ResearchItem> occupants = new ArrayList<>();
        for (ResearchItem existing : category.research.values()) {
            if (existing.isVirtual() && existing.displayColumn == movingResearch.displayColumn
                && existing.displayRow == movingResearch.displayRow) {
                occupants.add(existing);
            }
        }
        for (ResearchItem occupant : occupants) {
            category.research.remove(occupant.key);
        }
        return occupants;
    }
}
