package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.witcheryspinningwheel.SpinningRegistry;
import org.fentanylsolutions.thaumicdabblery.feature.witcheryspinningwheel.SpinningRegistry.Recipe;
import org.fentanylsolutions.thaumicdabblery.feature.witcheryspinningwheel.WitcherySpinningWheelFeature;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.witchery.SpinningWheel")
public final class WitcherySpinningWheelZen {

    private WitcherySpinningWheelZen() {}

    public static void init() {
        MineTweakerAPI.registerClass(WitcherySpinningWheelZen.class);
    }

    public static void registerCommand() {
        if (MineTweakerAPI.server != null) MineTweakerAPI.server.addMineTweakerCommand(
            "witcherySpinningWheel",
            new String[] { "/mt witcherySpinningWheel",
                "    Lists spinning wheel recipe IDs, inputs, outputs and modifiers in minetweaker.log" },
            (args, player) -> {
                dump();
                if (player != null) player.sendChat(
                    MineTweakerImplementationAPI.platform
                        .getMessage("Spinning Wheel recipes written to minetweaker.log"));
            });
    }

    private static ItemStack stack(IItemStack value, boolean input, boolean single) {
        if (value == null || value.hasTransformers())
            throw new IllegalArgumentException("expected an item without transformations");
        return SpinningRegistry.stack(MineTweakerMC.getItemStack(value), input, single);
    }

    private static ItemStack[] modifiers(IItemStack[] values) {
        if (values == null || values.length > 3) throw new IllegalArgumentException("expected zero to three modifiers");
        ItemStack[] result = new ItemStack[values.length];
        for (int i = 0; i < values.length; i++) result[i] = stack(values[i], true, true);
        return result;
    }

    private static int find(List<Recipe> rules, String id) {
        for (int i = 0; i < rules.size(); i++) if (id != null && id.equals(rules.get(i).id)) return i;
        throw new IllegalArgumentException("unknown spinning wheel recipe: " + id);
    }

    @ZenMethod
    public static void addRecipe(String id, IItemStack input, IItemStack[] modifiers, IItemStack output) {
        change("add " + id, rules -> {
            if (id == null || !id.matches("custom:[A-Za-z0-9_./-]+"))
                throw new IllegalArgumentException("recipe ID must start with custom:");
            for (Recipe r : rules)
                if (id.equals(r.id)) throw new IllegalArgumentException("duplicate recipe ID: " + id);
            rules.add(0, new Recipe(id, stack(input, true, false), modifiers(modifiers), stack(output, false, false)));
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
    public static void setInput(String id, IItemStack value) {
        edit(id, r -> r.input = stack(value, true, false));
    }

    @ZenMethod
    public static void setModifiers(String id, IItemStack[] values) {
        edit(id, r -> r.modifiers = modifiers(values));
    }

    @ZenMethod
    public static void setOutput(String id, IItemStack value) {
        edit(id, r -> r.output = stack(value, false, false));
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
                    if (!WitcherySpinningWheelFeature.isEnabled())
                        throw new IllegalArgumentException("spinning wheel scripting is disabled");
                    List<Recipe> old = SpinningRegistry.recipes(), next = new ArrayList<>(old);
                    operation.accept(next);
                    SpinningRegistry.replace(next);
                    before = old;
                } catch (IllegalArgumentException e) {
                    MineTweakerAPI.logError("Cannot " + description + " for Spinning Wheel: " + e.getMessage());
                }
            }

            public boolean canUndo() {
                return before != null;
            }

            public void undo() {
                if (before != null) {
                    SpinningRegistry.replace(before);
                    before = null;
                }
            }

            public String describe() {
                return "Spinning Wheel: " + description;
            }

            public String describeUndo() {
                return "Restoring Spinning Wheel: " + description;
            }

            public Object getOverrideKey() {
                return null;
            }
        });
    }

    @ZenMethod
    public static void dump() {
        MineTweakerAPI.logInfo("Spinning Wheel recipes (300 ticks; altar drain: 0.6 per working tick):");
        for (Recipe r : SpinningRegistry.recipes()) {
            StringBuilder line = new StringBuilder(r.id).append(" input=")
                .append(SpinningRegistry.key(r.input))
                .append(" * ")
                .append(r.input.stackSize)
                .append(" modifiers=[");
            for (ItemStack s : r.modifiers) line.append(SpinningRegistry.key(s))
                .append(" ");
            line.append("] output=")
                .append(SpinningRegistry.key(r.output))
                .append(" * ")
                .append(r.output.stackSize);
            if (r.output.hasTagCompound()) line.append(" NBT=")
                .append(r.output.getTagCompound());
            MineTweakerAPI.logInfo(line.toString());
        }
    }
}
