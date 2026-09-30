package org.fentanylsolutions.thaumicdabblery.mixins.late.modtweaker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import minetweaker.MineTweakerAPI;
import modtweaker2.mods.thaumcraft.research.OrphanResearch;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;

@Mixin(value = OrphanResearch.class, remap = false)
public abstract class MixinOrphanResearch {

    @Shadow
    private String key;

    @Unique
    private final Map<String, String[]> thaumicdabblery$parents = new LinkedHashMap<>();

    @Unique
    private final Map<String, String[]> thaumicdabblery$hiddenParents = new LinkedHashMap<>();

    @Unique
    private final Map<String, String[]> thaumicdabblery$siblings = new LinkedHashMap<>();

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$detachReferences(CallbackInfo ci) {
        thaumicdabblery$parents.clear();
        thaumicdabblery$hiddenParents.clear();
        thaumicdabblery$siblings.clear();

        for (ResearchCategoryList category : ResearchCategories.researchCategories.values()) {
            for (Map.Entry<String, ResearchItem> entry : category.research.entrySet()) {
                ResearchItem research = entry.getValue();
                research.setParents(
                    thaumicdabblery$removeReferences(entry.getKey(), research.parents, thaumicdabblery$parents));
                research.setParentsHidden(
                    thaumicdabblery$removeReferences(
                        entry.getKey(),
                        research.parentsHidden,
                        thaumicdabblery$hiddenParents));
                research.setSiblings(
                    thaumicdabblery$removeReferences(entry.getKey(), research.siblings, thaumicdabblery$siblings));
            }
        }
        // Both ModTweaker implementations remove only the first occurrence of a reference.
        ci.cancel();
    }

    @Inject(method = "canUndo", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$hasSavedReferences(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(
            !thaumicdabblery$parents.isEmpty() || !thaumicdabblery$hiddenParents.isEmpty()
                || !thaumicdabblery$siblings.isEmpty());
    }

    @Inject(method = "undo", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$restoreReferences(CallbackInfo ci) {
        thaumicdabblery$restore(thaumicdabblery$parents, ResearchItem::setParents);
        thaumicdabblery$restore(thaumicdabblery$hiddenParents, ResearchItem::setParentsHidden);
        thaumicdabblery$restore(thaumicdabblery$siblings, ResearchItem::setSiblings);
        ci.cancel();
    }

    @Unique
    private String[] thaumicdabblery$removeReferences(String researchKey, String[] references,
        Map<String, String[]> originals) {
        if (references == null || key == null) return references;
        int matches = 0;
        for (String reference : references) if (key.equals(reference)) matches++;
        if (matches == 0) return references;

        originals.put(researchKey, references.clone());
        String[] remaining = new String[references.length - matches];
        int index = 0;
        for (String reference : references) if (!key.equals(reference)) remaining[index++] = reference;
        return remaining;
    }

    @Unique
    private void thaumicdabblery$restore(Map<String, String[]> originals, BiConsumer<ResearchItem, String[]> setter) {
        for (Map.Entry<String, String[]> entry : originals.entrySet()) {
            ResearchItem research = ResearchCategories.getResearch(entry.getKey());
            if (research == null) {
                MineTweakerAPI.logWarning(
                    "Could not reattach missing Thaumcraft research " + entry.getKey() + " to " + key + ".");
            } else {
                setter.accept(
                    research,
                    entry.getValue()
                        .clone());
            }
        }
    }
}
