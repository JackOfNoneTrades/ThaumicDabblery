package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.Arrays;

import net.minecraft.entity.EntityList;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.researchscangates.ScanGateRegistry;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IItemStack;
import modtweaker2.helpers.InputHelper;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.thaumcraft.ResearchScanGates")
public final class ResearchScanGatesZen {

    private ResearchScanGatesZen() {}

    public static void register() {
        MineTweakerAPI.registerClass(ResearchScanGatesZen.class);
    }

    @ZenMethod
    public static void requireItem(String researchKey, IItemStack item) {
        MineTweakerAPI.apply(new ItemRequirementAction(researchKey, new ItemStack[] { requireItem(item) }));
    }

    @ZenMethod
    public static void requireAnyItem(String researchKey, IItemStack[] alternatives) {
        if (alternatives == null || alternatives.length == 0) {
            throw new IllegalArgumentException("Item scan alternatives cannot be empty");
        }
        ItemStack[] internal = new ItemStack[alternatives.length];
        for (int index = 0; index < alternatives.length; index++) {
            internal[index] = requireItem(alternatives[index]);
        }
        MineTweakerAPI.apply(new ItemRequirementAction(researchKey, internal));
    }

    @ZenMethod
    public static void requireEntity(String researchKey, String entityId) {
        MineTweakerAPI.apply(new EntityRequirementAction(researchKey, new String[] { requireEntity(entityId) }));
    }

    @ZenMethod
    public static void requireAnyEntity(String researchKey, String[] alternatives) {
        if (alternatives == null || alternatives.length == 0) {
            throw new IllegalArgumentException("Entity scan alternatives cannot be empty");
        }
        String[] internal = new String[alternatives.length];
        for (int index = 0; index < alternatives.length; index++) {
            internal[index] = requireEntity(alternatives[index]);
        }
        MineTweakerAPI.apply(new EntityRequirementAction(researchKey, internal));
    }

    @ZenMethod
    public static void clear(String researchKey) {
        MineTweakerAPI.apply(new ClearAction(researchKey));
    }

    private static ItemStack requireItem(IItemStack item) {
        ItemStack internal = InputHelper.toStack(item);
        if (internal == null || internal.getItem() == null) {
            throw new IllegalArgumentException("Item scan requirement cannot be null");
        }
        ItemStack result = internal.copy();
        result.stackSize = 1;
        return result;
    }

    private static String requireEntity(String entityId) {
        if (entityId == null || entityId.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Entity scan requirement cannot be empty");
        }
        String result = entityId.trim();
        if (!EntityList.stringToClassMapping.containsKey(result)) {
            throw new IllegalArgumentException("Unknown entity id: " + result);
        }
        return result;
    }

    private abstract static class ScanGateAction implements IUndoableAction {

        protected final String researchKey;
        protected ScanGateRegistry.Change change;

        private ScanGateAction(String researchKey) {
            this.researchKey = researchKey;
        }

        @Override
        public boolean canUndo() {
            return change != null;
        }

        @Override
        public void undo() {
            change.undo();
        }

        @Override
        public String describeUndo() {
            return "Restoring scan requirements for " + researchKey;
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }

    private static final class ItemRequirementAction extends ScanGateAction {

        private final ItemStack[] alternatives;

        private ItemRequirementAction(String researchKey, ItemStack[] alternatives) {
            super(researchKey);
            this.alternatives = alternatives;
        }

        @Override
        public void apply() {
            change = ScanGateRegistry.requireItems(researchKey, alternatives);
        }

        @Override
        public String describe() {
            return "Requiring a scan of any item in " + Arrays.toString(alternatives) + " for " + researchKey;
        }
    }

    private static final class EntityRequirementAction extends ScanGateAction {

        private final String[] alternatives;

        private EntityRequirementAction(String researchKey, String[] alternatives) {
            super(researchKey);
            this.alternatives = alternatives;
        }

        @Override
        public void apply() {
            change = ScanGateRegistry.requireEntities(researchKey, alternatives);
        }

        @Override
        public String describe() {
            return "Requiring a scan of any entity in " + Arrays.toString(alternatives) + " for " + researchKey;
        }
    }

    private static final class ClearAction extends ScanGateAction {

        private ClearAction(String researchKey) {
            super(researchKey);
        }

        @Override
        public void apply() {
            change = ScanGateRegistry.clear(researchKey);
        }

        @Override
        public String describe() {
            return "Clearing scripted scan requirements for " + researchKey;
        }
    }
}
