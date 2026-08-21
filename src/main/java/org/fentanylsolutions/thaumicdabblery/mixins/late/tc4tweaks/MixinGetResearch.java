package org.fentanylsolutions.thaumicdabblery.mixins.late.tc4tweaks;

import java.util.ConcurrentModificationException;

import net.glease.tc4tweak.modules.getResearch.GetResearch;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;

/**
 * Keeps TC4Tweaks' uncached research lookup safe while MineTweaker reloads mutate the shared research maps.
 */
@Mixin(value = GetResearch.class, remap = false)
public abstract class MixinGetResearch {

    @Unique
    private static final int MAX_CATEGORY_SNAPSHOT_ATTEMPTS = 8;

    @Inject(method = "getResearchSlow", at = @At("HEAD"), cancellable = true, require = 1)
    private static void thaumicdabblery$avoidLiveResearchIteration(String key,
        CallbackInfoReturnable<ResearchItem> cir) {
        for (int attempt = 0; attempt < MAX_CATEGORY_SNAPSHOT_ATTEMPTS; attempt++) {
            try {
                Object[] categories = ResearchCategories.researchCategories.values()
                    .toArray();
                for (Object value : categories) {
                    ResearchItem research = ((ResearchCategoryList) value).research.get(key);
                    if (research != null) {
                        cir.setReturnValue(research);
                        return;
                    }
                }
                cir.setReturnValue(null);
                return;
            } catch (ConcurrentModificationException ignored) {
                Thread.yield();
            }
        }

        // A tab itself is being repeatedly added or removed. A temporary miss is safer than crashing the client.
        cir.setReturnValue(null);
    }
}
