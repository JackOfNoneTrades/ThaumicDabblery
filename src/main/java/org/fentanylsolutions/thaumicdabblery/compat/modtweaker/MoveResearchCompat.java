package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.relauncher.ReflectionHelper;
import minetweaker.MineTweakerAPI;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;

public final class MoveResearchCompat {

    private MoveResearchCompat() {}

    public static boolean shouldRejectInvalidDestination(String key, String newTab, int x, int y) {
        ResearchItem movingResearch = ResearchCategories.getResearch(key);
        if (movingResearch == null) {
            return false;
        }

        ResearchCategoryList destination = ResearchCategories.getResearchList(newTab);
        if (destination == null) {
            MineTweakerAPI.logError(
                "Cannot move Thaumcraft research " + key
                    + " to unknown category "
                    + newTab
                    + ". The move was ignored.");
            return true;
        }
        if (movingResearch.isVirtual()) {
            return false;
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
                return true;
            }
        }
        return false;
    }

    public static ResearchItem registerIgnoringVirtualOccupants(ResearchItem movingResearch) {
        if (movingResearch.isVirtual()) {
            return movingResearch.registerResearchItem();
        }

        ResearchCategoryList destination = ResearchCategories.getResearchList(movingResearch.category);
        if (destination == null) {
            return movingResearch.registerResearchItem();
        }

        List<ResearchItem> virtualOccupants = removeVirtualOccupants(destination, movingResearch);
        try {
            return movingResearch.registerResearchItem();
        } finally {
            for (ResearchItem virtualResearch : virtualOccupants) {
                destination.research.put(virtualResearch.key, virtualResearch);
            }
        }
    }

    public static void setPositionAndCategory(ResearchItem research, int x, int y, String category) {
        ReflectionHelper.setPrivateValue(ResearchItem.class, research, x, "displayColumn");
        ReflectionHelper.setPrivateValue(ResearchItem.class, research, y, "displayRow");
        ReflectionHelper.setPrivateValue(ResearchItem.class, research, category, "category");
    }

    private static List<ResearchItem> removeVirtualOccupants(ResearchCategoryList category,
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
