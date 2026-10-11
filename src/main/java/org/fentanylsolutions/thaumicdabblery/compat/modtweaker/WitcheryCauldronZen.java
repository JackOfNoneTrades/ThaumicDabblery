package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.witcherycauldron.CauldronRecipes;
import org.fentanylsolutions.thaumicdabblery.feature.witcherycauldron.CauldronRecipes.Recipe;
import org.fentanylsolutions.thaumicdabblery.feature.witcherycauldron.WitcheryCauldronFeature;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.witchery.Cauldron")
public final class WitcheryCauldronZen {

    private WitcheryCauldronZen() {}

    public static void init() {
        MineTweakerAPI.registerClass(WitcheryCauldronZen.class);
    }

    public static void registerCommand() {
        if (MineTweakerAPI.server != null) MineTweakerAPI.server.addMineTweakerCommand(
            "witcheryCauldron",
            new String[] { "/mt witcheryCauldron",
                "    Lists cauldron recipe IDs, ingredients, triggers and base power in minetweaker.log" },
            (args, player) -> {
                dump();
                if (player != null) player.sendChat(
                    MineTweakerImplementationAPI.platform
                        .getMessage("Witches' Cauldron recipes written to minetweaker.log"));
            });
    }

    private static ItemStack input(IItemStack value) {
        if (value == null || value.hasTransformers())
            throw new IllegalArgumentException("expected a single exact item without transformations");
        return CauldronRecipes.input(MineTweakerMC.getItemStack(value));
    }

    private static ItemStack[] inputs(IItemStack[] values) {
        if (values == null || values.length > 5)
            throw new IllegalArgumentException("expected zero to five ordinary ingredients");
        ItemStack[] result = new ItemStack[values.length];
        for (int i = 0; i < values.length; i++) result[i] = input(values[i]);
        return result;
    }

    private static ItemStack output(IItemStack value) {
        ItemStack stack = value == null ? null : MineTweakerMC.getItemStack(value);
        if (stack == null || stack.getItem() == null
            || stack.getItemDamage() == 32767
            || stack.stackSize < 1
            || stack.stackSize > Math.min(64, stack.getMaxStackSize()))
            throw new IllegalArgumentException("output must be a concrete stack within its stack limit");
        return stack.copy();
    }

    private static int find(List<Recipe> rules, String id) {
        for (int i = 0; i < rules.size(); i++) if (id != null && id.equals(rules.get(i).id)) return i;
        throw new IllegalArgumentException("unknown cauldron recipe: " + id);
    }

    @ZenMethod
    public static void addRecipe(String id, IItemStack[] ingredients, IItemStack trigger, IItemStack output,
        @Optional(valueLong = 0) int basePower) {
        change("add " + id, rules -> {
            if (id == null || !id.matches("custom:[A-Za-z0-9_./-]+"))
                throw new IllegalArgumentException("recipe ID must start with custom:");
            for (Recipe r : rules)
                if (id.equals(r.id)) throw new IllegalArgumentException("duplicate recipe ID: " + id);
            rules.add(new Recipe(id, inputs(ingredients), input(trigger), output(output), basePower, false));
        });
    }

    @ZenMethod
    public static void setIngredients(String id, IItemStack[] ingredients) {
        change("ingredients " + id, rules -> {
            int i = find(rules, id);
            rules.set(
                i,
                rules.get(i)
                    .ingredients(inputs(ingredients)));
        });
    }

    @ZenMethod
    public static void setTrigger(String id, IItemStack trigger) {
        change("trigger " + id, rules -> {
            int i = find(rules, id);
            rules.set(
                i,
                rules.get(i)
                    .trigger(input(trigger)));
        });
    }

    @ZenMethod
    public static void setOutput(String id, IItemStack output) {
        change("output " + id, rules -> {
            int i = find(rules, id);
            rules.set(
                i,
                rules.get(i)
                    .output(output(output)));
        });
    }

    @ZenMethod
    public static void setPower(String id, int basePower) {
        change("power " + id, rules -> {
            int i = find(rules, id);
            rules.set(
                i,
                rules.get(i)
                    .power(basePower));
        });
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
                    if (!WitcheryCauldronFeature.isEnabled())
                        throw new IllegalArgumentException("cauldron scripting is disabled");
                    List<Recipe> old = CauldronRecipes.recipes(), next = new ArrayList<>(old);
                    operation.accept(next);
                    CauldronRecipes.replace(next);
                    before = old;
                } catch (IllegalArgumentException e) {
                    MineTweakerAPI.logError("Cannot " + description + " for Witches' Cauldron: " + e.getMessage());
                }
            }

            public boolean canUndo() {
                return before != null;
            }

            public void undo() {
                if (before != null) {
                    CauldronRecipes.replace(before);
                    before = null;
                }
            }

            public String describe() {
                return "Witches' Cauldron: " + description;
            }

            public String describeUndo() {
                return "Restoring Witches' Cauldron: " + description;
            }

            public Object getOverrideKey() {
                return null;
            }
        });
    }

    @ZenMethod
    public static void dump() {
        MineTweakerAPI.logInfo("Witches' Cauldron recipes (base power is before chalk-circle modifiers):");
        for (Recipe recipe : CauldronRecipes.recipes()) {
            StringBuilder line = new StringBuilder(recipe.id).append(" ingredients=[");
            for (ItemStack input : recipe.ingredients) line.append(CauldronRecipes.keyName(input))
                .append(" ");
            line.append("] trigger=")
                .append(CauldronRecipes.keyName(recipe.trigger))
                .append(" -> ")
                .append(CauldronRecipes.keyName(recipe.output))
                .append(" * ")
                .append(recipe.output.stackSize)
                .append("; base power=")
                .append(recipe.basePower());
            if (recipe.output.hasTagCompound()) line.append("; output NBT=")
                .append(recipe.output.getTagCompound());
            MineTweakerAPI.logInfo(line.toString());
        }
    }
}
