package org.fentanylsolutions.thaumicdabblery.mixins.late.modtweaker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import minetweaker.MineTweakerAPI;
import modtweaker2.mods.thaumcraft.research.AddPrereq;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;

@Mixin(value = AddPrereq.class, remap = false)
public abstract class MixinAddPrereq {

    @Shadow
    private String key;

    @Shadow
    private String[] oldPrereqs;

    @Shadow
    private boolean hidden;

    @Inject(method = "undo", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$restorePrerequisites(CallbackInfo ci) {
        ResearchItem research = ResearchCategories.getResearch(key);
        if (research == null) {
            MineTweakerAPI.logWarning(
                "Could not restore prerequisites for missing Thaumcraft research " + key + ". The undo was skipped.");
        } else if (oldPrereqs != null) {
            if (hidden) {
                research.setParentsHidden(oldPrereqs);
            } else {
                research.setParents(oldPrereqs);
            }
        }
        ci.cancel();
    }
}
