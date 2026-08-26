package org.fentanylsolutions.thaumicdabblery.mixins.late.modtweaker;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.MoveResearchCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import modtweaker2.mods.thaumcraft.research.MoveResearch;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;

@Mixin(value = MoveResearch.class, remap = false)
public abstract class MixinMoveResearchLegacy {

    @Shadow
    private String key;

    @Shadow
    private String newTab;

    @Shadow
    private int x;

    @Shadow
    private int y;

    @Shadow
    private String oldTab;

    @Shadow
    private int oldX;

    @Shadow
    private int oldY;

    @Shadow
    private boolean moved;

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$rejectInvalidDestination(CallbackInfo ci) {
        if (MoveResearchCompat.shouldRejectInvalidDestination(key, newTab, x, y)) {
            ci.cancel();
        }
    }

    @Inject(method = "undo", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$undoFromCurrentCategory(CallbackInfo ci) {
        ResearchItem research = ResearchCategories.getResearch(key);
        if (research != null) {
            ResearchCategoryList currentCategory = ResearchCategories.getResearchList(research.category);
            if (currentCategory != null) {
                currentCategory.research.remove(key);
            }
            MoveResearchCompat.setPositionAndCategory(research, oldX, oldY, oldTab);
            MoveResearchCompat.registerIgnoringVirtualOccupants(research);
        }
        moved = false;
        ci.cancel();
    }

    @Redirect(
        method = "apply",
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/api/research/ResearchItem;registerResearchItem()Lthaumcraft/api/research/ResearchItem;"),
        require = 1)
    private ResearchItem thaumicdabblery$registerIgnoringVirtualOccupants(ResearchItem movingResearch) {
        return MoveResearchCompat.registerIgnoringVirtualOccupants(movingResearch);
    }
}
