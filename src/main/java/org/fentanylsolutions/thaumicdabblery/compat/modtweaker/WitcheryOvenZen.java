package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.init.Items;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

import org.fentanylsolutions.thaumicdabblery.feature.witcheryoven.OvenRecipes;
import org.fentanylsolutions.thaumicdabblery.feature.witcheryoven.OvenRecipes.Input;
import org.fentanylsolutions.thaumicdabblery.feature.witcheryoven.OvenRecipes.Recipe;
import org.fentanylsolutions.thaumicdabblery.feature.witcheryoven.WitcheryOvenFeature;

import com.emoniph.witchery.Witchery;

import cpw.mods.fml.relauncher.ReflectionHelper;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.item.IngredientStack;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.api.oredict.IOreDictEntry;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.witchery.Oven")
public final class WitcheryOvenZen {

    private WitcheryOvenZen() {}

    public static void init() {
        MineTweakerAPI.registerClass(WitcheryOvenZen.class);
    }

    public static void registerCommand() {
        if (MineTweakerAPI.server != null) MineTweakerAPI.server.addMineTweakerCommand(
            "witcheryOven",
            new String[] { "/mt witcheryOven",
                "    Lists oven rules and native furnace candidates in minetweaker.log" },
            (args, player) -> {
                dump();
                if (player != null) player.sendChat(
                    MineTweakerImplementationAPI.platform
                        .getMessage("Witches' Oven recipes written to minetweaker.log"));
            });
    }

    private static Input input(IIngredient value) {
        if (value == null || value.getAmount() < 1 || value.getAmount() > 64 || value.hasTransformers())
            throw new IllegalArgumentException("input must contain 1..64 items without transformations");
        int amount = value.getAmount();
        while (value instanceof IngredientStack)
            value = ReflectionHelper.getPrivateValue(IngredientStack.class, (IngredientStack) value, "ingredient");
        if (value instanceof IOreDictEntry) {
            String ore = ((IOreDictEntry) value).getName();
            if (ore == null || ore.isEmpty()) throw new IllegalArgumentException("empty ore name");
            return new Input(null, ore, amount);
        }
        if (!(value instanceof IItemStack)) throw new IllegalArgumentException(
            "use an item or ore entry; conditions, alternatives and transformations are not supported");
        ItemStack stack = MineTweakerMC.getItemStack((IItemStack) value);
        if (stack == null || stack.getItem() == null
            || amount > stack.getMaxStackSize()
            || Witchery.Items.GENERIC.itemEmptyClayJar.isMatch(stack))
            throw new IllegalArgumentException("invalid input; empty clay jars are reserved for the jar slot");
        return new Input(stack, null, amount);
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
        throw new IllegalArgumentException("unknown oven recipe: " + id);
    }

    @ZenMethod
    public static void addRecipe(String id, IIngredient input, IItemStack output) {
        change("add " + id, rules -> {
            if (id == null || !id.matches("custom:[A-Za-z0-9_./-]+"))
                throw new IllegalArgumentException("recipe ID must start with custom:");
            for (Recipe r : rules)
                if (id.equals(r.id)) throw new IllegalArgumentException("duplicate recipe ID: " + id);
            rules.add(new Recipe(id, input(input), output(output), null, 0, 0.30, true, false));
        });
    }

    @ZenMethod
    public static void setByproduct(String id, IItemStack output, @Optional(valueLong = 1) int jars,
        @Optional(valueDouble = 0.30) double chance, @Optional(valueBoolean = true) boolean funnelBonus) {
        change("byproduct " + id, rules -> {
            int i = find(rules, id);
            if (jars < 0 || jars > 64 || !Double.isFinite(chance) || chance < 0 || chance > 1)
                throw new IllegalArgumentException("jars must be 0..64 and chance 0..1");
            if (rules.get(i).requireJars && jars == 0)
                throw new IllegalArgumentException("required jars must have a positive cost");
            rules.set(
                i,
                rules.get(i)
                    .byproduct(output(output), jars, chance, funnelBonus));
        });
    }

    @ZenMethod
    public static void clearByproduct(String id) {
        change("clear byproduct " + id, rules -> {
            int i = find(rules, id);
            rules.set(
                i,
                rules.get(i)
                    .byproduct(null, 0, 0, false));
        });
    }

    @ZenMethod
    public static void setRequireJars(String id, boolean required) {
        change("jar policy " + id, rules -> {
            int i = find(rules, id);
            Recipe r = rules.get(i);
            if (required && (r.byproduct == null || r.jars == 0))
                throw new IllegalArgumentException("define a byproduct with positive jar cost first");
            rules.set(i, r.required(required));
        });
    }

    @ZenMethod
    public static void removeRecipe(String id) {
        change("remove " + id, rules -> rules.remove(find(rules, id)));
    }

    @ZenMethod
    public static void removeInput(IIngredient input) {
        change("exclude input", rules -> rules.add(new Recipe(null, input(input), null, null, 0, 0, false, false)));
    }

    private static void change(String description, Consumer<List<Recipe>> operation) {
        MineTweakerAPI.apply(new IUndoableAction() {

            private List<Recipe> before;

            public void apply() {
                before = null;
                try {
                    if (!WitcheryOvenFeature.isEnabled())
                        throw new IllegalArgumentException("oven scripting is disabled");
                    List<Recipe> old = OvenRecipes.rules(), next = new ArrayList<>(old);
                    operation.accept(next);
                    OvenRecipes.replace(next);
                    before = old;
                } catch (IllegalArgumentException e) {
                    MineTweakerAPI.logError("Cannot " + description + " for Witches' Oven: " + e.getMessage());
                }
            }

            public boolean canUndo() {
                return before != null;
            }

            public void undo() {
                if (before != null) {
                    OvenRecipes.replace(before);
                    before = null;
                }
            }

            public String describe() {
                return "Witches' Oven: " + description;
            }

            public String describeUndo() {
                return "Restoring Witches' Oven: " + description;
            }

            public Object getOverrideKey() {
                return null;
            }
        });
    }

    @ZenMethod
    public static void dump() {
        MineTweakerAPI.logInfo("Witches' Oven rules (last matching add/exclusion wins):");
        for (Recipe r : OvenRecipes.rules()) MineTweakerAPI.logInfo(
            (r.output == null ? "EXCLUDE" : r.id) + " "
                + r.input.describe()
                + " -> "
                + OvenRecipes.itemName(r.output)
                + (r.output == null ? "" : " * " + r.output.stackSize)
                + "; byproduct="
                + OvenRecipes.itemName(r.byproduct)
                + (r.byproduct == null ? "" : " * " + r.byproduct.stackSize)
                + "; jars="
                + r.jars
                + "; chance="
                + r.chance
                + "; funnel bonus="
                + r.funnels
                + "; require jars="
                + r.requireJars);
        MineTweakerAPI.logInfo("Native furnace candidates without a scripted override (native fume rules apply):");
        for (Object object : FurnaceRecipes.smelting()
            .getSmeltingList()
            .entrySet()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) object;
            ItemStack in = (ItemStack) entry.getKey(), out = (ItemStack) entry.getValue();
            if (OvenRecipes.select(in) == null && (out.getItem() == Items.coal || out.getItem() instanceof ItemFood
                || Witchery.Items.GENERIC.itemAshWood.isMatch(out)))
                MineTweakerAPI
                    .logInfo(OvenRecipes.itemName(in) + " -> " + OvenRecipes.itemName(out) + " * " + out.stackSize);
        }
    }
}
