package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import org.fentanylsolutions.thaumicdabblery.feature.aurapylon.AuraPylonRegistry;

import cpw.mods.fml.common.Loader;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.gadomancy.AuraPylon")
public final class AuraPylonZen {

    private AuraPylonZen() {}

    public static void register() {
        MineTweakerAPI.registerClass(AuraPylonZen.class);
    }

    @ZenMethod
    public static void add(String aspect, String target, int potionId) {
        add(aspect, target, potionId, 0, 10, 1200, 4, 8);
    }

    @ZenMethod
    public static void add(String aspect, String target, int potionId, int amplifier, int addedTicks, int maxTicks,
        int intervalTicks, double range) {
        requireGadomancy();
        String tag = AuraPylonRegistry.requireAspect(aspect)
            .getTag();
        AuraPylonRegistry.Rule rule = AuraPylonRegistry
            .rule(target, potionId, amplifier, addedTicks, maxTicks, intervalTicks, range);
        MineTweakerAPI.apply(
            new Action(
                tag,
                "Adding Aura Pylon potion " + potionId + " for " + target,
                () -> AuraPylonRegistry.add(tag, rule)));
    }

    @ZenMethod
    public static void clear(String aspect) {
        requireGadomancy();
        String tag = AuraPylonRegistry.requireAspect(aspect)
            .getTag();
        MineTweakerAPI.apply(new Action(tag, "Clearing all Aura Pylon effects", () -> AuraPylonRegistry.clear(tag)));
    }

    @ZenMethod
    public static void remove(String aspect) {
        clear(aspect);
    }

    @ZenMethod
    public static void removeCustom(String aspect, String target, int potionId) {
        requireGadomancy();
        String tag = AuraPylonRegistry.requireAspect(aspect)
            .getTag();
        String selector = AuraPylonRegistry.requireTarget(target);
        AuraPylonRegistry.requirePotion(potionId);
        MineTweakerAPI.apply(
            new Action(
                tag,
                "Removing scripted Aura Pylon potion " + potionId + " for " + selector,
                () -> AuraPylonRegistry.removeCustom(tag, selector, potionId)));
    }

    private static void requireGadomancy() {
        if (!Loader.isModLoaded("gadomancy")) throw new IllegalStateException("AuraPylon requires Gadomancy");
    }

    private static final class Action implements IUndoableAction {

        private final String description;
        private final java.util.function.Supplier<Runnable> apply;
        private Runnable undo;

        private Action(String aspect, String description, java.util.function.Supplier<Runnable> apply) {
            this.description = description + " for aspect " + aspect;
            this.apply = apply;
        }

        @Override
        public void apply() {
            undo = apply.get();
        }

        @Override
        public boolean canUndo() {
            return undo != null;
        }

        @Override
        public void undo() {
            if (undo != null) {
                undo.run();
                undo = null;
            }
        }

        @Override
        public String describe() {
            return description;
        }

        @Override
        public String describeUndo() {
            return "Undo: " + description;
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }
}
