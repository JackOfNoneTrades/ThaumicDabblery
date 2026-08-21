package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.lib.research.ResearchManager;

public final class ResearchTabGateRegistry {

    private static final String ELDRITCH_CATEGORY = "ELDRITCH";
    private static final String ELDRITCH_RESEARCH = "ELDRITCHMINOR";

    private static final Map<String, String> GATES = new LinkedHashMap<>();

    private static boolean warnedAboutEmptyResult;

    private ResearchTabGateRegistry() {}

    public static synchronized Change set(String categoryKey, String researchKey) {
        requireCategory(categoryKey);
        requireResearch(researchKey);
        validateCanSet(categoryKey);

        String previous = GATES.put(categoryKey, researchKey);
        return new GateChange(categoryKey, researchKey, previous);
    }

    static synchronized void validateCanSet(String categoryKey) {
        if (!GATES.containsKey(categoryKey) && !hasOtherAlwaysVisibleCategory(categoryKey)) {
            throw new IllegalArgumentException(
                "Cannot gate Thaumcraft research category " + categoryKey
                    + " because no other tab would remain visible");
        }
    }

    public static synchronized Change clear(String categoryKey) {
        requireCategory(categoryKey);

        String previous = GATES.remove(categoryKey);
        return new GateChange(categoryKey, null, previous);
    }

    public static synchronized Set<String> getVisibleCategories(Set<String> categories, String playerName) {
        Set<String> visible = new LinkedHashSet<>();
        for (String category : categories) {
            if (isVisible(category, playerName)) {
                visible.add(category);
            }
        }

        String fallback = getFallbackIfEveryCategoryIsHidden(playerName);
        if (visible.isEmpty() && fallback != null && categories.contains(fallback)) {
            visible.add(fallback);
        }

        if (fallback != null) {
            if (!warnedAboutEmptyResult) {
                ThaumicDabblery.LOG.warn(
                    "Every Thaumcraft research category is hidden for {}. Showing {} as a safety fallback.",
                    playerName,
                    fallback);
                warnedAboutEmptyResult = true;
            }
        } else {
            warnedAboutEmptyResult = false;
        }
        return visible;
    }

    public static synchronized boolean isVisible(String categoryKey, String playerName) {
        if (ELDRITCH_CATEGORY.equals(categoryKey) && !isResearchComplete(playerName, ELDRITCH_RESEARCH)) {
            return false;
        }

        String researchKey = GATES.get(categoryKey);
        return researchKey == null || isResearchComplete(playerName, researchKey);
    }

    private static boolean isResearchComplete(String playerName, String researchKey) {
        return playerName != null && !playerName.isEmpty()
            && ResearchManager.isResearchComplete(playerName, researchKey);
    }

    private static String getFallbackIfEveryCategoryIsHidden(String playerName) {
        String fallback = null;
        for (String category : ResearchCategories.researchCategories.keySet()) {
            if (fallback == null) {
                fallback = category;
            }
            if (isVisible(category, playerName)) {
                return null;
            }
        }
        return fallback;
    }

    private static void requireCategory(String categoryKey) {
        if (ResearchCategories.getResearchList(categoryKey) == null) {
            throw new IllegalArgumentException("Unknown Thaumcraft research category: " + categoryKey);
        }
    }

    private static void requireResearch(String researchKey) {
        if (ResearchCategories.getResearch(researchKey) == null) {
            throw new IllegalArgumentException("Unknown Thaumcraft research: " + researchKey);
        }
    }

    private static boolean hasOtherAlwaysVisibleCategory(String excludedCategory) {
        for (String category : ResearchCategories.researchCategories.keySet()) {
            if (!category.equals(excludedCategory) && !ELDRITCH_CATEGORY.equals(category)
                && !GATES.containsKey(category)) {
                return true;
            }
        }
        return false;
    }

    public interface Change {

        void undo();
    }

    private static final class GateChange implements Change {

        private final String categoryKey;
        private final String applied;
        private final String previous;

        private boolean undone;

        private GateChange(String categoryKey, String applied, String previous) {
            this.categoryKey = categoryKey;
            this.applied = applied;
            this.previous = previous;
        }

        @Override
        public void undo() {
            synchronized (ResearchTabGateRegistry.class) {
                if (undone) {
                    return;
                }
                if (!Objects.equals(GATES.get(categoryKey), applied)) {
                    throw new IllegalStateException(
                        "Cannot restore research gate for " + categoryKey + " because it changed unexpectedly");
                }
                if (previous == null) {
                    GATES.remove(categoryKey);
                } else {
                    GATES.put(categoryKey, previous);
                }
                undone = true;
            }
        }
    }
}
