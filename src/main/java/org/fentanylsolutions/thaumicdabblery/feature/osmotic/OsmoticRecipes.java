package org.fentanylsolutions.thaumicdabblery.feature.osmotic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.research.ResearchManager;
import thaumic.tinkerer.common.block.tile.TileEnchanter;
import thaumic.tinkerer.common.enchantment.core.EnchantmentData;
import thaumic.tinkerer.common.enchantment.core.EnchantmentManager;

public final class OsmoticRecipes {

    private static final Aspect[] PRIMALS = { Aspect.AIR, Aspect.EARTH, Aspect.FIRE, Aspect.WATER, Aspect.ORDER,
        Aspect.ENTROPY };

    public static boolean validWand(ItemStack wand) {
        if (wand == null || wand.stackSize <= 0 || !(wand.getItem() instanceof ItemWandCasting)) return false;
        ItemWandCasting item = (ItemWandCasting) wand.getItem();
        return !item.isStaff(wand) && item.getCap(wand)
            .getBaseCostModifier() <= 1F;
    }

    /** Native payment rounds each one-vis charge separately, without player equipment discounts. */
    public static int unitCost(ItemStack wand, Aspect aspect) {
        float modifier = ((ItemWandCasting) wand.getItem()).getConsumptionModifier(wand, null, aspect, true);
        return Float.isNaN(modifier) || Float.isInfinite(modifier) || modifier < 0 ? -1 : (int) (100 * modifier);
    }

    public static long missingVis(TileEnchanter tile, Aspect aspect) {
        ItemStack wand = tile.getStackInSlot(1);
        int units = tile.totalAspects.getAmount(aspect);
        if (units <= 0) return 0;
        if (!validWand(wand)) return (long) units * 100;
        int cost = unitCost(wand, aspect);
        if (cost < 0) return Long.MAX_VALUE;
        return Math.max(0L, (long) units * cost - ((ItemWandCasting) wand.getItem()).getVis(wand, aspect));
    }

    public static boolean canAfford(TileEnchanter tile) {
        if (!validWand(tile.getStackInSlot(1))) return false;
        for (Aspect aspect : PRIMALS) if (missingVis(tile, aspect) != 0) return false;
        return true;
    }

    /** Feed the native one-vis gate the actual discounted affordability of each payment. */
    public static AspectList payableAspects(ItemStack wand) {
        AspectList result = new AspectList();
        if (validWand(wand)) for (Aspect aspect : PRIMALS) {
            int cost = unitCost(wand, aspect);
            if (cost >= 0 && ((ItemWandCasting) wand.getItem()).getVis(wand, aspect) >= cost) result.add(aspect, 100);
        }
        return result;
    }

    public static void cancel(TileEnchanter tile, boolean keepSelection) {
        if (!tile.working || tile.getWorldObj() == null || tile.getWorldObj().isRemote) return;
        tile.working = false;
        tile.currentAspects = new AspectList();
        if (keepSelection) refresh(tile);
        else {
            tile.enchantments.clear();
            tile.levels.clear();
            tile.totalAspects = new AspectList();
        }
        sync(tile);
        tile.getWorldObj()
            .playSoundEffect(tile.xCoord + 0.5, tile.yCoord + 0.5, tile.zCoord + 0.5, "thaumcraft:craftfail", 1F, 1F);
    }

    public static Enchantment enchantment(int id) {
        return id < 0 || id >= Enchantment.enchantmentsList.length ? null : Enchantment.enchantmentsList[id];
    }

    public static boolean standardPrimal(Aspect aspect) {
        return aspect == Aspect.AIR || aspect == Aspect.EARTH
            || aspect == Aspect.FIRE
            || aspect == Aspect.WATER
            || aspect == Aspect.ORDER
            || aspect == Aspect.ENTROPY;
    }

    public static AspectList copy(AspectList source) {
        AspectList copy = new AspectList();
        if (source != null)
            for (Aspect aspect : source.getAspects()) if (aspect != null) copy.add(aspect, source.getAmount(aspect));
        return copy;
    }

    public static EnchantmentData data(int id, int level) {
        Map<Integer, EnchantmentData> levels = EnchantmentManager.enchantmentData.get(id);
        return levels == null ? null : levels.get(level);
    }

    public static boolean usable(EntityPlayer player, int id) {
        EnchantmentData data = data(id, 1);
        return enchantment(id) != null && data != null
            && (data.research.isEmpty()
                || ResearchManager.isResearchComplete(player.getCommandSenderName(), data.research));
    }

    public static boolean applies(ItemStack stack, int id, List<Integer> selected) {
        Enchantment enchant = enchantment(id);
        if (enchant != null) for (Integer otherId : selected) {
            Enchantment other = enchantment(otherId);
            if (other == null || !enchant.canApplyTogether(other) || !other.canApplyTogether(enchant)) return false;
        }
        return stack != null && !stack.isItemEnchanted()
            && stack.getItem()
                .getItemEnchantability() != 0
            && enchant != null
            && enchant.type != null
            && EnchantmentManager.canApply(stack, enchant, selected);
    }

    public static List<Integer> available(ItemStack stack, EntityPlayer player, List<Integer> selected) {
        List<Integer> result = new ArrayList<>();
        for (Integer id : EnchantmentManager.enchantmentData.keySet())
            if (usable(player, id) && applies(stack, id, selected)) result.add(id);
        java.util.Collections.sort(result);
        return result;
    }

    /** Running jobs retain the already accepted total, including across saves and script reloads. */
    public static boolean refresh(TileEnchanter tile) {
        List<Integer> ids = new ArrayList<>(), levels = new ArrayList<>();
        ItemStack stack = tile.getStackInSlot(0);
        for (int i = 0; i < Math.min(tile.enchantments.size(), tile.levels.size()); i++) {
            int id = tile.enchantments.get(i), level = tile.levels.get(i);
            Enchantment enchant = enchantment(id);
            if (stack == null || enchant == null || level < 1 || level > enchant.getMaxLevel() || ids.contains(id))
                continue;
            if (!tile.working && (data(id, level) == null || !applies(stack, id, ids))) continue;
            ids.add(id);
            levels.add(level);
        }
        boolean changed = !ids.equals(tile.enchantments) || !levels.equals(tile.levels);
        if (tile.working && changed) { // Corrupt or missing enchantments cannot be completed safely.
            tile.working = false;
            ids.clear();
            levels.clear();
        }
        tile.enchantments.clear();
        tile.enchantments.addAll(ids);
        tile.levels.clear();
        tile.levels.addAll(levels);
        if (!tile.working) {
            AspectList total = new AspectList();
            for (int i = 0; i < ids.size(); i++) {
                AspectList cost = data(ids.get(i), levels.get(i)).aspects;
                for (Aspect aspect : cost.getAspects()) if (aspect != null) {
                    long amount = (long) total.getAmount(aspect) + cost.getAmount(aspect);
                    if (amount < 0 || amount > Integer.MAX_VALUE) {
                        tile.enchantments.clear();
                        tile.levels.clear();
                        tile.totalAspects = new AspectList();
                        tile.currentAspects = new AspectList();
                        org.fentanylsolutions.thaumicdabblery.ThaumicDabblery.LOG.warn(
                            "Cleared invalid Osmotic total cost at {},{},{}",
                            tile.xCoord,
                            tile.yCoord,
                            tile.zCoord);
                        return true;
                    }
                    total.add(aspect, cost.getAmount(aspect));
                }
            }
            changed |= !total.aspects.equals(tile.totalAspects.aspects) || tile.currentAspects.size() != 0;
            tile.totalAspects = total;
            tile.currentAspects = new AspectList();
        }
        return changed;
    }

    public static void sync(TileEnchanter tile) {
        if (tile.getWorldObj() == null || tile.getWorldObj().isRemote) return;
        tile.getWorldObj()
            .markTileEntityChunkModified(tile.xCoord, tile.yCoord, tile.zCoord, tile);
        tile.getWorldObj()
            .markBlockForUpdate(tile.xCoord, tile.yCoord, tile.zCoord);
    }

    public static void select(TileEnchanter tile, EntityPlayer player, int id, int level) {
        if (tile.working) return;
        refresh(tile);
        int index = tile.enchantments.indexOf(id);
        if (level == -1 && index >= 0) {
            tile.enchantments.remove(index);
            tile.levels.remove(index);
        } else if (usable(player, id)) {
            if (index < 0 && (level == 0 || level == 1) && applies(tile.getStackInSlot(0), id, tile.enchantments)) {
                tile.enchantments.add(id);
                tile.levels.add(1);
            } else if (index >= 0 && level >= 1 && level <= enchantment(id).getMaxLevel() && data(id, level) != null)
                tile.levels.set(index, level);
        }
        refresh(tile);
        sync(tile);
    }

    public static void start(TileEnchanter tile, EntityPlayer player) {
        if (tile.working) return;
        refresh(tile);
        boolean rejected = false;
        for (int i = tile.enchantments.size() - 1; i >= 0; i--) if (!usable(player, tile.enchantments.get(i))) {
            tile.enchantments.remove(i);
            tile.levels.remove(i);
            rejected = true;
        }
        refresh(tile);
        if (!rejected && !tile.enchantments.isEmpty() && canAfford(tile)) tile.working = true;
        sync(tile);
    }
}
