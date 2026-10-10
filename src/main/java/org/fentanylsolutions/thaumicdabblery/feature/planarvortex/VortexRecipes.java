package org.fentanylsolutions.thaumicdabblery.feature.planarvortex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

import com.kentington.thaumichorizons.common.ThaumicHorizons;
import com.kentington.thaumichorizons.common.tiles.TileVortex;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.wands.ItemWandCasting;

/** The runtime hook deliberately has no MineTweaker dependencies. */
public final class VortexRecipes {

    public static final Map<String, Recipe> RECIPES = new LinkedHashMap<>();
    public static final Set<String> DISABLED = new HashSet<>();
    private static final Set<Result> FAILED = Collections.newSetFromMap(new WeakHashMap<>());
    private static final String OUTPUT_TAG = "thaumicdabblery:vortexOutput";

    public interface Holder {

        List<NBTTagCompound> thaumicdabblery$pending();
    }

    public static List<NBTTagCompound> pending(TileVortex vortex) {
        return ((Holder) vortex).thaumicdabblery$pending();
    }

    public static class Result {

        public final ItemStack output;
        public final String entity;
        public final NBTTagCompound nbt;
        public final AspectList vis;

        Result(ItemStack output, String entity, NBTTagCompound nbt, AspectList vis) {
            this.output = output == null ? null : output.copy();
            this.entity = entity;
            this.nbt = (NBTTagCompound) nbt.copy();
            // TC4's copy() turns an empty list into a null-aspect entry.
            this.vis = new AspectList();
            if (vis.size() > 0) for (Aspect aspect : vis.getAspects()) {
                if (aspect != null && vis.getAmount(aspect) > 0) this.vis.add(aspect, vis.getAmount(aspect));
            }
        }

        private NBTTagCompound write() {
            NBTTagCompound data = new NBTTagCompound();
            if (output != null) data.setTag("item", output.writeToNBT(new NBTTagCompound()));
            if (entity != null) data.setString("entity", entity);
            data.setTag("nbt", nbt.copy());
            NBTTagCompound cost = new NBTTagCompound();
            if (vis.size() > 0)
                for (Aspect aspect : vis.getAspects()) cost.setInteger(aspect.getTag(), vis.getAmount(aspect));
            data.setTag("vis", cost);
            return data;
        }

        private static Result read(NBTTagCompound data) {
            ItemStack output = data.hasKey("item") ? ItemStack.loadItemStackFromNBT(data.getCompoundTag("item")) : null;
            if (data.hasKey("item") && (output == null || output.stackSize <= 0 || output.stackSize > 64))
                throw new IllegalArgumentException("Missing or invalid saved vortex output item");
            AspectList vis = new AspectList();
            NBTTagCompound cost = data.getCompoundTag("vis");
            for (Object key : cost.func_150296_c()) {
                String name = (String) key;
                Aspect aspect = Aspect.getAspect(name);
                int amount = cost.getInteger(name);
                if (aspect == null || !aspect.isPrimal() || amount <= 0 || amount > 100000)
                    throw new IllegalArgumentException("Invalid saved vortex vis cost: " + name);
                vis.add(aspect, amount);
            }
            return new Result(output, data.getString("entity"), data.getCompoundTag("nbt"), vis);
        }
    }

    public static final class Recipe extends Result {

        public final ItemStack input;
        public final String completion;

        public Recipe(ItemStack input, ItemStack output, String entity, NBTTagCompound nbt) {
            this(input, output, entity, nbt, output == null ? "instant" : "native", new AspectList());
        }

        private Recipe(ItemStack input, ItemStack output, String entity, NBTTagCompound nbt, String completion,
            AspectList vis) {
            super(output, entity, nbt, vis);
            this.input = input.copy();
            this.completion = completion;
        }

        public Recipe withCompletion(String mode, AspectList cost) {
            return new Recipe(input, output, entity, nbt, mode, cost);
        }

        private boolean matches(ItemStack stack) {
            return stack.getItem() == input.getItem()
                && (input.getItemDamage() == 32767 || input.getItemDamage() == stack.getItemDamage())
                && (!input.hasTagCompound() || input.getTagCompound()
                    .equals(stack.getTagCompound()));
        }
    }

    public static String builtin(ItemStack input) {
        if (input.getItem() == ConfigItems.itemResource) {
            if (input.getItemDamage() == 16) return "builtin:void_putty";
            if (input.getItemDamage() == 14) return "builtin:wisps";
        }
        if (input.getItem() == ThaumicHorizons.itemCrystalWand) return "builtin:crystal_wand";
        if (input.getItem() == ThaumicHorizons.itemGolemPowder) return "builtin:void_golem";
        return null;
    }

    /** Only actual releases animate; accepting a wand recipe into its queue stays immediate. */
    public static boolean animatesInput(TileVortex vortex, EntityItem item) {
        if (item.isDead || isOutput(vortex, item)) return false;
        ItemStack stack = item.getEntityItem();
        if (stack == null || stack.stackSize <= 0) return false;
        List<Recipe> ordered = new ArrayList<>(RECIPES.values());
        for (int i = ordered.size() - 1; i >= 0; i--) {
            Recipe recipe = ordered.get(i);
            if (recipe.matches(stack))
                return stack.stackSize >= recipe.input.stackSize && "instant".equals(recipe.completion);
        }
        String builtin = builtin(stack);
        return !DISABLED.contains(builtin) && ("builtin:wisps".equals(builtin)
            || "builtin:void_golem".equals(builtin) && item.func_145800_j() != null);
    }

    public static boolean canComplete(TileVortex vortex, ItemStack stack, EntityPlayer player) {
        if (stack == null || !(stack.getItem() instanceof ItemWandCasting)) return false;
        if (!vortex.items.isEmpty()) return true;
        if (pending(vortex).isEmpty()) return false;
        try {
            Result result = Result.read(pending(vortex).get(0));
            return result.vis.size() == 0
                || ((ItemWandCasting) stack.getItem()).consumeAllVisCrafting(stack, player, result.vis, false);
        } catch (IllegalArgumentException e) {
            return false; // The normal click path reports malformed saved results.
        }
    }

    /** True claims the input even if it is too small or an entity spawn was cancelled. */
    public static boolean handle(TileVortex vortex, EntityItem item) {
        if (vortex.getWorldObj().isRemote || item.isDead || isOutput(vortex, item)) return true;
        ItemStack stack = item.getEntityItem();
        if (stack == null || stack.stackSize <= 0) return true;
        List<Recipe> ordered = new ArrayList<>(RECIPES.values());
        for (int i = ordered.size() - 1; i >= 0; i--) {
            Recipe recipe = ordered.get(i);
            if (!recipe.matches(stack)) continue;
            int batches = stack.stackSize / recipe.input.stackSize;
            for (int batch = 0; batch < batches; batch++) {
                if ("wand".equals(recipe.completion)) pending(vortex).add(((Result) recipe).write());
                else if (!produce(vortex, recipe, "instant".equals(recipe.completion))) break;
                stack.stackSize -= recipe.input.stackSize;
                vortex.markDirty();
            }
            if (stack.stackSize <= 0) item.setDead();
            else if (batches > 0) item.setEntityItemStack(stack.copy());
            return true;
        }
        return DISABLED.contains(builtin(stack));
    }

    /** Process one saved batch per click. Native queued items retain their original extraction behavior. */
    public static boolean click(TileVortex vortex, ItemStack stack, EntityPlayer player) {
        if (!vortex.items.isEmpty() || pending(vortex).isEmpty()) return false;
        if (vortex.getWorldObj().isRemote) return true;
        if (vortex.collapsing || vortex.createdDimension
            || vortex.count < 50
            || !vortex.cheat && vortex.beams < 6
                && vortex.getWorldObj().provider.dimensionId != ThaumicHorizons.dimensionPocketId) {
            fail(vortex);
            return true;
        }
        if (stack == null || !(stack.getItem() instanceof ItemWandCasting)) return true;
        Result result;
        try {
            result = Result.read(pending(vortex).get(0));
        } catch (IllegalArgumentException e) {
            ThaumicDabblery.LOG.error(
                "Invalid queued vortex result at " + vortex.xCoord + "," + vortex.yCoord + "," + vortex.zCoord,
                e);
            fail(vortex);
            return true;
        }
        ItemWandCasting wand = (ItemWandCasting) stack.getItem();
        NBTTagCompound previous = stack.hasTagCompound() ? (NBTTagCompound) stack.getTagCompound()
            .copy() : null;
        if (result.vis.size() > 0 && !wand.consumeAllVisCrafting(stack, player, result.vis, true)) {
            fail(vortex);
            return true;
        }
        if (!produce(vortex, result, true)) {
            stack.setTagCompound(previous);
            ThaumicDabblery.LOG.warn(
                "Vortex output could not spawn at {},{},{}; retained offering and refunded vis",
                vortex.xCoord,
                vortex.yCoord,
                vortex.zCoord);
            fail(vortex);
        } else {
            pending(vortex).remove(0);
            vortex.markDirty();
            player.swingItem();
        }
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
        return true;
    }

    private static void fail(TileVortex vortex) {
        vortex.getWorldObj()
            .playSoundEffect(
                vortex.xCoord + 0.5,
                vortex.yCoord + 0.5,
                vortex.zCoord + 0.5,
                "thaumcraft:wandfail",
                0.1F,
                0.8F + vortex.getWorldObj().rand.nextFloat() * 0.1F);
    }

    private static boolean isOutput(TileVortex vortex, EntityItem item) {
        NBTTagCompound tag = item.getEntityData()
            .getCompoundTag(OUTPUT_TAG);
        return item.getEntityData()
            .hasKey(OUTPUT_TAG) && tag.getInteger("dimension") == vortex.getWorldObj().provider.dimensionId
            && tag.getInteger("x") == vortex.xCoord
            && tag.getInteger("y") == vortex.yCoord
            && tag.getInteger("z") == vortex.zCoord;
    }

    private static boolean produce(TileVortex vortex, Result recipe, boolean eject) {
        List<Entity> spawned = new ArrayList<>();
        try {
            if (recipe.output != null) {
                int remaining = recipe.output.stackSize;
                while (remaining > 0) {
                    ItemStack output = recipe.output.copy();
                    int size = output.getMaxStackSize();
                    if (size <= 0) throw new IllegalArgumentException("Vortex output has an invalid stack limit");
                    output.stackSize = Math.min(remaining, size);
                    if (eject) {
                        EntityItem entity = new EntityItem(
                            vortex.getWorldObj(),
                            vortex.xCoord + 0.5,
                            vortex.yCoord + 0.5,
                            vortex.zCoord + 0.5,
                            output);
                        NBTTagCompound tag = new NBTTagCompound();
                        tag.setInteger("dimension", vortex.getWorldObj().provider.dimensionId);
                        tag.setInteger("x", vortex.xCoord);
                        tag.setInteger("y", vortex.yCoord);
                        tag.setInteger("z", vortex.zCoord);
                        entity.getEntityData()
                            .setTag(OUTPUT_TAG, tag);
                        entity.delayBeforeCanPickup = 10;
                        if (!vortex.getWorldObj()
                            .spawnEntityInWorld(entity)) {
                            for (Entity previous : spawned) previous.setDead();
                            return false;
                        }
                        spawned.add(entity);
                    } else vortex.items.add(output);
                    remaining -= output.stackSize;
                }
                return true;
            }
            EntityLiving entity = (EntityLiving) EntityList.createEntityByName(recipe.entity, vortex.getWorldObj());
            if (entity == null) return false;
            entity.setPosition(vortex.xCoord + 0.5, vortex.yCoord + 0.5, vortex.zCoord + 0.5);
            entity.onSpawnWithEgg(null);
            if (!recipe.nbt.hasNoTags()) {
                NBTTagCompound data = new NBTTagCompound();
                entity.writeToNBT(data);
                merge(data, recipe.nbt);
                entity.readFromNBT(data);
            }
            entity.setPosition(vortex.xCoord + 0.5, vortex.yCoord + 0.5, vortex.zCoord + 0.5);
            return vortex.getWorldObj()
                .spawnEntityInWorld(entity);
        } catch (RuntimeException e) {
            for (Entity previous : spawned) previous.setDead();
            if (FAILED.add(recipe)) ThaumicDabblery.LOG.error("Unable to create vortex output " + recipe.entity, e);
            return false;
        }
    }

    private static void merge(NBTTagCompound target, NBTTagCompound patch) {
        for (Object key : patch.func_150296_c()) {
            String name = (String) key;
            NBTBase value = patch.getTag(name);
            if (value instanceof NBTTagCompound && target.getTag(name) instanceof NBTTagCompound)
                merge(target.getCompoundTag(name), (NBTTagCompound) value);
            else target.setTag(name, value.copy());
        }
    }
}
