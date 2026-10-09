package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import org.fentanylsolutions.thaumicdabblery.compat.thaumichorizons.CustomCreatureRecipe;
import org.fentanylsolutions.thaumicdabblery.feature.planarvortex.VortexRecipes;
import org.fentanylsolutions.thaumicdabblery.feature.planarvortex.VortexRecipes.Recipe;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.data.IData;
import minetweaker.api.item.IItemStack;
import minetweaker.mc1710.data.NBTConverter;
import modtweaker2.helpers.InputHelper;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ConfigItems;

@ZenClass("mods.thaumichorizons.PlanarVortex")
public final class PlanarVortexZen {

    public static void register() {
        MineTweakerAPI.registerClass(PlanarVortexZen.class);
    }

    @ZenMethod
    public static void addItemRecipe(String key, IItemStack input, IItemStack output) {
        add(key, new Recipe(stack(input, true), stack(output, false), null, new NBTTagCompound()));
    }

    @ZenMethod
    public static void addEntityRecipe(String key, IItemStack input, String entity) {
        addEntityRecipe(key, input, entity, null);
    }

    @ZenMethod
    public static void addEntityRecipe(String key, IItemStack input, String entity, IData data) {
        CustomCreatureRecipe.requireMob(entity, false);
        NBTBase converted = data == null ? new NBTTagCompound() : NBTConverter.from(data);
        if (!(converted instanceof NBTTagCompound)) throw new IllegalArgumentException("Entity NBT must be a compound");
        NBTTagCompound nbt = (NBTTagCompound) converted;
        for (String name : new String[] { "id", "Pos", "Motion", "Rotation", "Dimension", "UUIDMost", "UUIDLeast",
            "Riding" })
            if (nbt.hasKey(name)) throw new IllegalArgumentException("Output entity NBT cannot override " + name);
        add(key, new Recipe(stack(input, true), null, entity, nbt));
    }

    @ZenMethod
    public static void setCompletion(String key, String mode) {
        setCompletion(key, mode, "");
    }

    @ZenMethod
    public static void setCompletion(String key, String mode, String vis) {
        Recipe previous = VortexRecipes.RECIPES.get(key);
        if (previous == null) throw new IllegalArgumentException("Unknown custom vortex recipe: " + key);
        if (!"instant".equals(mode) && !"wand".equals(mode))
            throw new IllegalArgumentException("Vortex completion must be instant or wand");
        if (vis == null) throw new IllegalArgumentException("Vortex vis cost cannot be null");
        AspectList cost = new AspectList();
        if (!vis.trim()
            .isEmpty()) for (String part : vis.split(",", -1)) {
                String[] words = part.trim()
                    .split("\\s+");
                if (words.length != 2) throw new IllegalArgumentException("Invalid vis cost: " + part);
                Aspect aspect = Aspect.getAspect(words[0]);
                int amount = Integer.parseInt(words[1]);
                if (aspect == null || !aspect.isPrimal()
                    || amount <= 0
                    || amount > 100000
                    || cost.getAmount(aspect) > 100000 - amount)
                    throw new IllegalArgumentException(
                        "Vis costs require primal aspects and amounts from 1 to 100000: " + part);
                cost.add(aspect, amount);
            }
        if ("instant".equals(mode) && cost.size() > 0)
            throw new IllegalArgumentException("Instant vortex recipes cannot charge a wand vis cost");
        Recipe replacement = previous.withCompletion(mode, cost);
        MineTweakerAPI.apply(new Change(key) {

            public void apply() {
                VortexRecipes.RECIPES.put(key, replacement);
            }

            public void undo() {
                VortexRecipes.RECIPES.put(key, previous);
            }
        });
    }

    private static ItemStack stack(IItemStack ingredient, boolean input) {
        ItemStack stack = ingredient == null ? null : InputHelper.toStack(ingredient);
        if (stack == null || stack.getItem() == null || stack.stackSize < 1 || stack.stackSize > 64)
            throw new IllegalArgumentException("Vortex items require a stack size from 1 to 64");
        if (!input && stack.getItemDamage() == 32767)
            throw new IllegalArgumentException("Vortex outputs require concrete metadata");
        if (input && stack.getItem() == ConfigItems.itemEldritchObject
            && (stack.getItemDamage() == 3 || stack.getItemDamage() == 32767))
            throw new IllegalArgumentException("Primordial pearls are reserved for pocket-plane creation");
        return stack.copy();
    }

    private static void add(String key, Recipe recipe) {
        if (key == null || !key.matches("custom:[A-Za-z0-9_./-]+") || VortexRecipes.RECIPES.containsKey(key))
            throw new IllegalArgumentException("Expected a unique custom:<name> vortex recipe key: " + key);
        MineTweakerAPI.apply(new Change(key) {

            public void apply() {
                VortexRecipes.RECIPES.put(key, recipe);
            }

            public void undo() {
                VortexRecipes.RECIPES.remove(key);
            }
        });
    }

    @ZenMethod
    public static void removeRecipe(String key) {
        if (Arrays.asList("builtin:void_putty", "builtin:wisps", "builtin:crystal_wand", "builtin:void_golem")
            .contains(key)) {
            MineTweakerAPI.apply(new Change(key) {

                boolean changed;

                public void apply() {
                    changed = VortexRecipes.DISABLED.add(key);
                }

                public void undo() {
                    if (changed) VortexRecipes.DISABLED.remove(key);
                }
            });
        } else {
            if (!VortexRecipes.RECIPES.containsKey(key))
                throw new IllegalArgumentException("Unknown vortex recipe: " + key);
            MineTweakerAPI.apply(new Change(key) {

                Map<String, Recipe> previous;

                public void apply() {
                    previous = new LinkedHashMap<>(VortexRecipes.RECIPES);
                    VortexRecipes.RECIPES.remove(key);
                }

                public void undo() {
                    VortexRecipes.RECIPES.clear();
                    VortexRecipes.RECIPES.putAll(previous);
                }
            });
        }
    }

    private abstract static class Change implements IUndoableAction {

        private final String key;

        Change(String key) {
            this.key = key;
        }

        public boolean canUndo() {
            return true;
        }

        public String describe() {
            return "Apply vortex recipe change: " + key;
        }

        public String describeUndo() {
            return "Restore vortex recipe: " + key;
        }

        public Object getOverrideKey() {
            return null;
        }
    }
}
