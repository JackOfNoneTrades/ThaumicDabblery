package org.fentanylsolutions.thaumicdabblery.mixins.late.modtweaker;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    private final Set<String> thaumicdabblery$children = new LinkedHashSet<>();

    @Unique
    private final Set<String> thaumicdabblery$secretChildren = new LinkedHashSet<>();

    @Unique
    private final Set<String> thaumicdabblery$siblings = new LinkedHashSet<>();

    @Inject(method = "apply", at = @At("HEAD"))
    private void thaumicdabblery$captureReferences(CallbackInfo ci) {
        thaumicdabblery$children.clear();
        thaumicdabblery$secretChildren.clear();
        thaumicdabblery$siblings.clear();

        for (ResearchCategoryList category : ResearchCategories.researchCategories.values()) {
            for (Map.Entry<String, ResearchItem> entry : category.research.entrySet()) {
                ResearchItem research = entry.getValue();
                if (thaumicdabblery$containsReference(research.parents)) {
                    thaumicdabblery$children.add(entry.getKey());
                }
                if (thaumicdabblery$containsReference(research.parentsHidden)) {
                    thaumicdabblery$secretChildren.add(entry.getKey());
                }
                if (thaumicdabblery$containsReference(research.siblings)) {
                    thaumicdabblery$siblings.add(entry.getKey());
                }
            }
        }
    }

    @Inject(method = "undo", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$restoreReferences(CallbackInfo ci) {
        thaumicdabblery$restoreParents(thaumicdabblery$children, false);
        thaumicdabblery$restoreParents(thaumicdabblery$secretChildren, true);
        thaumicdabblery$restoreSiblings();
        ci.cancel();
    }

    @Unique
    private boolean thaumicdabblery$containsReference(String[] references) {
        if (references == null) {
            return false;
        }
        for (String reference : references) {
            if (key == null ? reference == null : key.equals(reference)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private void thaumicdabblery$restoreParents(Set<String> researchKeys, boolean hidden) {
        for (String researchKey : researchKeys) {
            ResearchItem research = thaumicdabblery$getResearch(researchKey);
            if (research == null) {
                continue;
            }
            if (hidden) {
                research.setParentsHidden(thaumicdabblery$appendReference(research.parentsHidden));
            } else {
                research.setParents(thaumicdabblery$appendReference(research.parents));
            }
        }
    }

    @Unique
    private void thaumicdabblery$restoreSiblings() {
        for (String researchKey : thaumicdabblery$siblings) {
            ResearchItem research = thaumicdabblery$getResearch(researchKey);
            if (research != null) {
                research.setSiblings(thaumicdabblery$appendReference(research.siblings));
            }
        }
    }

    @Unique
    private ResearchItem thaumicdabblery$getResearch(String researchKey) {
        ResearchItem research = ResearchCategories.getResearch(researchKey);
        if (research == null) {
            MineTweakerAPI
                .logWarning("Could not reattach missing Thaumcraft research " + researchKey + " to " + key + ".");
        }
        return research;
    }

    @Unique
    private String[] thaumicdabblery$appendReference(String[] references) {
        if (references != null) {
            for (String reference : references) {
                if (key == null ? reference == null : key.equals(reference)) {
                    return references;
                }
            }
        }

        int oldLength = references == null ? 0 : references.length;
        String[] restored = references == null ? new String[1] : Arrays.copyOf(references, oldLength + 1);
        restored[oldLength] = key;
        return restored;
    }
}
