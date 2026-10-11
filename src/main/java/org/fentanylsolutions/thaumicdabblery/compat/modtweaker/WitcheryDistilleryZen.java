package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.witcherydistillery.DistilleryRegistry;
import org.fentanylsolutions.thaumicdabblery.feature.witcherydistillery.DistilleryRegistry.Recipe;
import org.fentanylsolutions.thaumicdabblery.feature.witcherydistillery.WitcheryDistilleryFeature;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.witchery.Distillery")
public final class WitcheryDistilleryZen {

    private WitcheryDistilleryZen() {}

    public static void init() {
        MineTweakerAPI.registerClass(WitcheryDistilleryZen.class);
    }

    public static void registerCommand() {
        if (MineTweakerAPI.server != null) MineTweakerAPI.server.addMineTweakerCommand(
            "witcheryDistillery",
            new String[] { "/mt witcheryDistillery",
                "    Lists distillery recipe IDs, inputs, outputs and jar costs in minetweaker.log" },
            (args, player) -> {
                dump();
                if (player != null) player.sendChat(
                    MineTweakerImplementationAPI.platform
                        .getMessage("Witches' Distillery recipes written to minetweaker.log"));
            });
    }

    private static ItemStack input(IItemStack value) {
        if (value == null || value.hasTransformers())
            throw new IllegalArgumentException("expected a single exact item without transformations");
        return DistilleryRegistry.input(MineTweakerMC.getItemStack(value));
    }

    private static ItemStack[] inputs(IItemStack[] values) {
        if (values == null || values.length < 1 || values.length > 2)
            throw new IllegalArgumentException("expected one or two inputs");
        ItemStack[] result = new ItemStack[2];
        for (int i = 0; i < values.length; i++) result[i] = input(values[i]);
        return result;
    }

    private static ItemStack[] outputs(IItemStack[] values) {
        if (values == null || values.length < 1 || values.length > 4)
            throw new IllegalArgumentException("expected one to four outputs");
        ItemStack[] result = new ItemStack[4];
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) throw new IllegalArgumentException("outputs cannot contain null entries");
            ItemStack s = MineTweakerMC.getItemStack(values[i]);
            if (s == null) throw new IllegalArgumentException("invalid output");
            result[i] = s.copy();
        }
        return result;
    }

    private static int find(List<Recipe> rules, String id) {
        for (int i = 0; i < rules.size(); i++) if (id != null && id.equals(rules.get(i).id)) return i;
        throw new IllegalArgumentException("unknown distillery recipe: " + id);
    }

    @ZenMethod
    public static void addRecipe(String id, IItemStack[] inputs, IItemStack[] outputs,
        @Optional(valueLong = 0) int jars) {
        change("add " + id, rules -> {
            if (id == null || !id.matches("custom:[A-Za-z0-9_./-]+"))
                throw new IllegalArgumentException("recipe ID must start with custom:");
            for (Recipe r : rules)
                if (id.equals(r.id)) throw new IllegalArgumentException("duplicate recipe ID: " + id);
            rules.add(0, new Recipe(id, inputs(inputs), outputs(outputs), jars));
        });
    }

    private static void edit(String id, Consumer<Recipe> operation) {
        change("edit " + id, rules -> {
            int i = find(rules, id);
            Recipe r = rules.get(i)
                .copy();
            operation.accept(r);
            rules.set(i, r);
        });
    }

    @ZenMethod
    public static void setInputs(String id, IItemStack[] values) {
        edit(id, r -> r.inputs = inputs(values));
    }

    @ZenMethod
    public static void setOutputs(String id, IItemStack[] values) {
        edit(id, r -> r.outputs = outputs(values));
    }

    @ZenMethod
    public static void setJars(String id, int jars) {
        edit(id, r -> r.jars = jars);
    }

    @ZenMethod
    public static void removeRecipe(String id) {
        change("remove " + id, rules -> rules.remove(find(rules, id)));
    }

    private static void change(String description, Consumer<List<Recipe>> operation) {
        MineTweakerAPI.apply(new IUndoableAction() {

            private List<Recipe> before;

            public void apply() {
                before = null;
                try {
                    if (!WitcheryDistilleryFeature.isEnabled())
                        throw new IllegalArgumentException("distillery scripting is disabled");
                    List<Recipe> old = DistilleryRegistry.recipes(), next = new ArrayList<>(old);
                    operation.accept(next);
                    DistilleryRegistry.replace(next);
                    before = old;
                } catch (IllegalArgumentException e) {
                    MineTweakerAPI.logError("Cannot " + description + " for Witches' Distillery: " + e.getMessage());
                }
            }

            public boolean canUndo() {
                return before != null;
            }

            public void undo() {
                if (before != null) {
                    DistilleryRegistry.replace(before);
                    before = null;
                }
            }

            public String describe() {
                return "Witches' Distillery: " + description;
            }

            public String describeUndo() {
                return "Restoring Witches' Distillery: " + description;
            }

            public Object getOverrideKey() {
                return null;
            }
        });
    }

    @ZenMethod
    public static void dump() {
        MineTweakerAPI
            .logInfo("Witches' Distillery recipes (native speed: 800 ticks; altar drain: 0.6 per working tick):");
        for (Recipe r : DistilleryRegistry.recipes()) {
            StringBuilder line = new StringBuilder(r.id).append(" inputs=[");
            for (ItemStack s : r.inputs) if (s != null) line.append(DistilleryRegistry.key(s))
                .append(" ");
            line.append("] outputs=[");
            for (ItemStack s : r.outputs) if (s != null) {
                line.append(DistilleryRegistry.key(s))
                    .append(" * ")
                    .append(s.stackSize);
                if (s.hasTagCompound()) line.append(" NBT=")
                    .append(s.getTagCompound());
                line.append(" ");
            }
            MineTweakerAPI.logInfo(
                line.append("]; jars=")
                    .append(r.jars)
                    .toString());
        }
    }
}
