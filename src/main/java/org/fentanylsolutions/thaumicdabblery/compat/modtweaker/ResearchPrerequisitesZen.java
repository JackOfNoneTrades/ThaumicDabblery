package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethodStatic;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;

/** Edits one prerequisite without disturbing other links or losing the original lists on reload. */
@ZenExpansion("mods.thaumcraft.Research")
public final class ResearchPrerequisitesZen {

    private ResearchPrerequisitesZen() {}

    public static void register() {
        MineTweakerAPI.registerClass(ResearchPrerequisitesZen.class);
    }

    @ZenMethodStatic
    public static void removePrereq(String key, String prereq) {
        MineTweakerAPI.apply(new Change(key, prereq, false, true));
    }

    public static IUndoableAction update(String key, String prereq, boolean hidden) {
        return new Change(key, prereq, hidden, false);
    }

    private static String[] copy(String[] values) {
        return values == null ? null : values.clone();
    }

    private static String[] rewrite(String[] values, String prereq, boolean include) {
        if (values == null && !include) return null;
        List<String> result = new ArrayList<>();
        boolean found = false;
        if (values != null) {
            for (String value : values) {
                if (!Objects.equals(value, prereq)) result.add(value);
                else if (include && !found) {
                    result.add(value);
                    found = true;
                }
            }
        }
        if (include && !found) result.add(prereq);
        return result.toArray(new String[0]);
    }

    private static final class Change implements IUndoableAction {

        private final String key, prereq;
        private final boolean hidden, remove;
        private String[] parents, hiddenParents;
        private boolean applied;

        private Change(String key, String prereq, boolean hidden, boolean remove) {
            this.key = key;
            this.prereq = prereq;
            this.hidden = hidden;
            this.remove = remove;
        }

        @Override
        public void apply() {
            applied = false;
            if (key == null || key.trim()
                .isEmpty()
                || prereq == null
                || prereq.trim()
                    .isEmpty()) {
                MineTweakerAPI.logError("Cannot edit prerequisite: research keys must not be empty");
                return;
            }
            ResearchItem research = ResearchCategories.getResearch(key);
            if (research == null) {
                MineTweakerAPI.logError("Cannot edit prerequisites for missing Thaumcraft research " + key);
                return;
            }
            parents = copy(research.parents);
            hiddenParents = copy(research.parentsHidden);
            research.parents = rewrite(research.parents, prereq, !remove && !hidden);
            research.parentsHidden = rewrite(research.parentsHidden, prereq, !remove && hidden);
            applied = true;
        }

        @Override
        public void undo() {
            if (!applied) return;
            ResearchItem research = ResearchCategories.getResearch(key);
            if (research == null) {
                MineTweakerAPI.logWarning("Could not restore prerequisites for missing Thaumcraft research " + key);
            } else {
                research.parents = copy(parents);
                research.parentsHidden = copy(hiddenParents);
            }
            applied = false;
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public String describe() {
            return (remove ? "Removing" : "Setting") + " prerequisite " + prereq + " for " + key;
        }

        @Override
        public String describeUndo() {
            return "Restoring prerequisites for " + key;
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }
}
