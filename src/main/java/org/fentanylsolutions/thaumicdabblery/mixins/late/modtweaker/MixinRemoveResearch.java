package org.fentanylsolutions.thaumicdabblery.mixins.late.modtweaker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import modtweaker2.mods.thaumcraft.research.OrphanResearch;
import modtweaker2.mods.thaumcraft.research.RemoveResearch;
import thaumcraft.api.research.ResearchCategories;

@Mixin(value = RemoveResearch.class, remap = false)
public abstract class MixinRemoveResearch {

    @Shadow
    private String key;

    @Unique
    private OrphanResearch thaumicdabblery$orphanAction;

    @Inject(method = "apply", at = @At("HEAD"))
    private void thaumicdabblery$detachReferences(CallbackInfo ci) {
        if (ResearchCategories.getResearch(key) == null) {
            return;
        }

        thaumicdabblery$orphanAction = new OrphanResearch(key);
        thaumicdabblery$orphanAction.apply();
    }

    @Inject(method = "undo", at = @At("TAIL"))
    private void thaumicdabblery$restoreReferences(CallbackInfo ci) {
        if (thaumicdabblery$orphanAction != null && thaumicdabblery$orphanAction.canUndo()) {
            thaumicdabblery$orphanAction.undo();
        }
    }
}
