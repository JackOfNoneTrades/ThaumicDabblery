package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import org.fentanylsolutions.thaumicdabblery.feature.warpevents.CustomWarpEvents;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.thaumcraft.WarpEvents")
public final class WarpEventsZen {

    private WarpEventsZen() {}

    public static void init() {
        MineTweakerAPI.registerClass(WarpEventsZen.class);
    }

    @ZenMethod
    public static void register(String name, int minWarp, int maxWarp, String[] commands) {
        try {
            MineTweakerAPI.apply(new Add(new CustomWarpEvents.Event(name, minWarp, maxWarp, commands)));
        } catch (IllegalArgumentException error) {
            MineTweakerAPI.logError("Cannot register warp event " + name + ": " + error.getMessage());
        }
    }

    private static final class Add implements IUndoableAction {

        private final CustomWarpEvents.Event event;
        private boolean applied;

        private Add(CustomWarpEvents.Event event) {
            this.event = event;
        }

        public void apply() {
            try {
                CustomWarpEvents.add(event);
                applied = true;
            } catch (IllegalArgumentException error) {
                MineTweakerAPI.logError(error.getMessage());
            }
        }

        public boolean canUndo() {
            return applied;
        }

        public void undo() {
            if (applied) CustomWarpEvents.undo(event);
            applied = false;
        }

        public String describe() {
            return "Registering warp event " + event.name;
        }

        public String describeUndo() {
            return "Removing warp event " + event.name;
        }

        public Object getOverrideKey() {
            return null;
        }
    }
}
