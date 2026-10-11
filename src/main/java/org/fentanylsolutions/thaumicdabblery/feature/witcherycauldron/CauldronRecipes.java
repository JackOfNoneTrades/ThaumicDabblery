package org.fentanylsolutions.thaumicdabblery.feature.witcherycauldron;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.mixins.late.witchery.BrewRegistryAccessor;
import org.fentanylsolutions.thaumicdabblery.mixins.late.witchery.CauldronRecipeAccessor;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.brewing.AltarPower;
import com.emoniph.witchery.brewing.BrewItemKey;
import com.emoniph.witchery.brewing.EffectLevelCounter;
import com.emoniph.witchery.brewing.WitcheryBrewRegistry;
import com.emoniph.witchery.brewing.action.BrewAction;
import com.emoniph.witchery.brewing.action.BrewActionEffect;
import com.emoniph.witchery.brewing.action.BrewActionList;
import com.emoniph.witchery.brewing.action.BrewActionRitualRecipe;
import com.emoniph.witchery.brewing.action.BrewPotionEffect;

public final class CauldronRecipes {

    public static final String PENDING = "ThaumicDabbleryCauldronRecipe";
    private static List<Recipe> baseline;
    private static volatile List<Recipe> recipes;
    private static volatile boolean changed;
    private static final Set<BrewItemKey> nativeTriggers = new HashSet<>();
    private static final Map<BrewItemKey, BrewAction> installed = new HashMap<>();

    private CauldronRecipes() {}

    private static Hashtable<BrewItemKey, BrewAction> actions() {
        return ((BrewRegistryAccessor) WitcheryBrewRegistry.INSTANCE).td$getIngredients();
    }

    public static synchronized List<Recipe> recipes() {
        if (baseline == null) {
            List<Recipe> initial = new ArrayList<>();
            for (BrewActionRitualRecipe action : WitcheryBrewRegistry.INSTANCE.getRecipes()) {
                nativeTriggers.add(action.ITEM_KEY);
                int index = 0;
                for (BrewActionRitualRecipe.Recipe recipe : ((CauldronRecipeAccessor) action).td$getRecipes()) {
                    ItemStack trigger = action.ITEM_KEY.toStack();
                    String id = "native:" + Item.itemRegistry
                        .getNameForObject(trigger.getItem()) + "/" + trigger.getItemDamage() + "/" + index++;
                    initial.add(new Recipe(id, recipe.ingredients, trigger, recipe.result, null, true));
                }
            }
            baseline = Collections.unmodifiableList(initial);
            recipes = baseline;
        }
        return recipes;
    }

    public static boolean active() {
        return changed && WitcheryCauldronFeature.isEnabled();
    }

    public static boolean manages(BrewItemKey key) {
        return active() && (nativeTriggers.contains(key) || installed.containsKey(key));
    }

    public static final class Recipe {

        public final String id;
        public final ItemStack[] ingredients;
        public final ItemStack trigger, output;
        public final Integer power;
        public final boolean nativeRecipe;

        public Recipe(String id, ItemStack[] ingredients, ItemStack trigger, ItemStack output, Integer power,
            boolean nativeRecipe) {
            this.id = id;
            this.ingredients = new ItemStack[ingredients.length];
            for (int i = 0; i < ingredients.length; i++) this.ingredients[i] = ingredients[i].copy();
            this.trigger = trigger.copy();
            this.output = output.copy();
            this.power = power;
            this.nativeRecipe = nativeRecipe;
        }

        public Recipe ingredients(ItemStack[] value) {
            return new Recipe(id, value, trigger, output, power, nativeRecipe);
        }

        public Recipe trigger(ItemStack value) {
            return new Recipe(id, ingredients, value, output, power, nativeRecipe);
        }

        public Recipe output(ItemStack value) {
            return new Recipe(id, ingredients, trigger, value, power, nativeRecipe);
        }

        public Recipe power(int value) {
            return new Recipe(id, ingredients, trigger, output, value, nativeRecipe);
        }

        public int basePower() {
            if (power != null) return power;
            long total = Math.max(0, WitcheryBrewRegistry.INSTANCE.getAltarPower(trigger));
            for (ItemStack in : ingredients) total += Math.max(0, WitcheryBrewRegistry.INSTANCE.getAltarPower(in));
            return (int) Math.min(Integer.MAX_VALUE, total);
        }
    }

    public static Recipe select(BrewItemKey trigger, BrewActionList list) {
        if (!active()) return null;
        List<Recipe> current = recipes;
        for (int i = current.size() - 1; i >= 0; i--) {
            Recipe r = current.get(i);
            if (!r.nativeRecipe && matches(r, trigger, list)) return r;
        }
        for (Recipe r : current) if (r.nativeRecipe && matches(r, trigger, list)) return r;
        return null;
    }

    private static boolean matches(Recipe recipe, BrewItemKey trigger, BrewActionList list) {
        if (!BrewItemKey.fromStack(recipe.trigger)
            .equals(trigger)) return false;
        List<BrewItemKey> remaining = new ArrayList<>();
        for (BrewAction action : list.actions) remaining.add(action.ITEM_KEY);
        for (ItemStack input : recipe.ingredients) if (!remaining.remove(BrewItemKey.fromStack(input))) return false;
        return true;
    }

    public static boolean visible(Recipe recipe) {
        BrewItemKey trigger = BrewItemKey.fromStack(recipe.trigger);
        if (!manages(trigger)) return true;
        NBTTagCompound brew = new NBTTagCompound();
        NBTTagList items = new NBTTagList();
        for (ItemStack input : recipe.ingredients) items.appendTag(input.writeToNBT(new NBTTagCompound()));
        brew.setTag("Items", items);
        return select(trigger, new BrewActionList(brew, actions())) == recipe;
    }

    public static Recipe selectBeforeAdd(BrewAction action, NBTTagCompound brew) {
        return action instanceof BrewActionRitualRecipe && manages(action.ITEM_KEY)
            ? select(action.ITEM_KEY, new BrewActionList(brew, actions()))
            : null;
    }

    /** Build and validate everything before touching Witchery's shared action table. */
    public static synchronized void replace(List<Recipe> next) {
        recipes();
        Hashtable<BrewItemKey, BrewAction> plan = new Hashtable<>(actions());
        for (Map.Entry<BrewItemKey, BrewAction> e : installed.entrySet())
            if (plan.get(e.getKey()) == e.getValue()) plan.remove(e.getKey());
        Map<BrewItemKey, BrewAction> additions = new HashMap<>();
        Set<BrewItemKey> triggers = new HashSet<>();
        for (Recipe r : next) triggers.add(BrewItemKey.fromStack(r.trigger));
        for (Recipe r : next) {
            boolean original = baseline.contains(r);
            if (!original) {
                input(r.trigger);
                if (r.ingredients.length > 5)
                    throw new IllegalArgumentException("at most five ingredients plus a trigger");
                if (r.power != null && (r.power < 0 || r.power > Integer.MAX_VALUE / 2))
                    throw new IllegalArgumentException("base power must be 0.." + Integer.MAX_VALUE / 2);
            }
            BrewItemKey key = BrewItemKey.fromStack(r.trigger);
            BrewAction existing = plan.get(key);
            if (existing != null && !(existing instanceof BrewActionRitualRecipe))
                throw new IllegalArgumentException("trigger already has another brewing role: " + keyName(r.trigger));
            if (existing == null) {
                BrewAction added = installed.get(key);
                if (!(added instanceof BrewActionRitualRecipe))
                    added = new BrewActionRitualRecipe(key, new AltarPower(0));
                plan.put(key, added);
                additions.put(key, added);
            }
        }
        for (Recipe r : next) {
            Set<BrewItemKey> used = new HashSet<>();
            used.add(BrewItemKey.fromStack(r.trigger));
            for (ItemStack in : r.ingredients) {
                if (!baseline.contains(r)) input(in);
                BrewItemKey key = BrewItemKey.fromStack(in);
                if (!baseline.contains(r) && (!used.add(key) || triggers.contains(key)))
                    throw new IllegalArgumentException(
                        "duplicate ingredient or ingredient used as a recipe trigger: " + keyName(in));
                BrewAction existing = plan.get(key);
                if (existing == null) {
                    BrewAction added = installed.get(key);
                    if (!(added instanceof CauldronIngredient)) added = new CauldronIngredient(key);
                    plan.put(key, added);
                    additions.put(key, added);
                } else if (!baseline.contains(r) && existing.triggersRitual())
                    throw new IllegalArgumentException("ingredient starts another ritual: " + keyName(in));
            }
        }
        for (Recipe r : next) if (!baseline.contains(r)) {
            try {
                validateOrder(r, plan);
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Witchery could not validate ingredient order for " + r.id, e);
            }
        }
        // Preserve unrelated additions made by other mods; undo removes only our exact objects.
        for (Map.Entry<BrewItemKey, BrewAction> e : installed.entrySet())
            if (actions().get(e.getKey()) == e.getValue()) actions().remove(e.getKey());
        actions().putAll(additions);
        installed.clear();
        installed.putAll(additions);
        recipes = Collections.unmodifiableList(new ArrayList<>(next));
        changed = !next.equals(baseline);
    }

    public static ItemStack input(ItemStack stack) {
        if (stack == null || stack.getItem() == null
            || stack.stackSize != 1
            || stack.getItemDamage() < 0
            || stack.getItemDamage() == 32767
            || stack.hasTagCompound())
            throw new IllegalArgumentException("ingredients and trigger must be single, exact items without NBT");
        if (Witchery.Items.GENERIC.itemGypsum.isMatch(stack) || Witchery.Items.GENERIC.itemQuicklime.isMatch(stack))
            throw new IllegalArgumentException("gypsum and quicklime have special cauldron behavior");
        return stack.copy();
    }

    private static void validateOrder(Recipe recipe, Hashtable<BrewItemKey, BrewAction> plan) {
        NBTTagCompound brew = new NBTTagCompound();
        NBTTagList items = new NBTTagList();
        brew.setTag("Items", items);
        for (ItemStack input : recipe.ingredients) {
            BrewAction next = plan.get(BrewItemKey.fromStack(input));
            try {
                if (next.getClass()
                    .getMethod("removeWhenAddedToCauldron", World.class)
                    .getDeclaringClass() != BrewAction.class)
                    throw new IllegalArgumentException("ingredient has special removal behavior: " + keyName(input));
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            BrewActionList list = new BrewActionList(brew, plan);
            EffectLevelCounter effects = new EffectLevelCounter();
            boolean found = false;
            for (BrewAction action : list.actions) {
                action.augmentEffectLevels(effects);
                if (action == next) found = true;
                else if (action instanceof BrewActionEffect || action instanceof BrewPotionEffect) found = false;
            }
            if (found || !next.canAdd(list, true, effects.hasEffects()) || !next.augmentEffectLevels(effects))
                throw new IllegalArgumentException("Witchery rejects ingredient in this order: " + keyName(input));
            next.processNullifaction(list.actions, items);
            items.appendTag(input.writeToNBT(new NBTTagCompound()));
        }
        if (!matches(recipe, BrewItemKey.fromStack(recipe.trigger), new BrewActionList(brew, plan)))
            throw new IllegalArgumentException("ingredients cancel each other before the trigger can be added");
    }

    public static String keyName(ItemStack stack) {
        return "<" + Item.itemRegistry.getNameForObject(stack.getItem()) + ":" + stack.getItemDamage() + ">";
    }
}
