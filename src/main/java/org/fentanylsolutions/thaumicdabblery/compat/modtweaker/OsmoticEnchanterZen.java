package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.util.ResourceLocation;

import org.fentanylsolutions.thaumicdabblery.feature.osmotic.OsmoticRecipes;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumic.tinkerer.common.enchantment.core.EnchantmentData;
import thaumic.tinkerer.common.enchantment.core.EnchantmentManager;

@ZenClass("mods.thaumictinkerer.OsmoticEnchanter")
public final class OsmoticEnchanterZen {

    public static void register() {
        MineTweakerAPI.registerClass(OsmoticEnchanterZen.class);
    }

    @ZenMethod
    public static void setEnchantment(int id, String baseCost, String icon) {
        setEnchantment(id, baseCost, icon, "");
    }

    @ZenMethod
    public static void setEnchantment(int id, String baseCost, String icon, String research) {
        Enchantment enchant = require(id);
        AspectList base = cost(baseCost);
        String texture = icon(icon), gate = research(research);
        Map<Integer, EnchantmentData> levels = new LinkedHashMap<>();
        for (int level = 1; level <= enchant.getMaxLevel(); level++) {
            AspectList scaled = new AspectList();
            for (Aspect aspect : base.getAspects()) {
                double amount = base.getAmount(aspect) * (level * (1D + level * .2D));
                if (amount > 1000000)
                    throw new IllegalArgumentException("Osmotic cost exceeds 1000000 vis per aspect and level");
                scaled.add(aspect, (int) amount);
            }
            levels.put(level, new EnchantmentData(texture, false, scaled, gate));
        }
        change(id, levels);
    }

    @ZenMethod
    public static void setLevelCost(int id, int level, String vis) {
        Enchantment enchant = require(id);
        if (level < 1 || level > enchant.getMaxLevel())
            throw new IllegalArgumentException("Invalid enchantment level: " + level);
        Map<Integer, EnchantmentData> levels = existing(id);
        EnchantmentData old = levels.get(level);
        if (old == null)
            throw new IllegalArgumentException("No Osmotic recipe for enchantment " + id + " level " + level);
        levels.put(level, new EnchantmentData(old.texture.toString(), old.vanilla, cost(vis), old.research));
        change(id, levels);
    }

    @ZenMethod
    public static void setResearch(int id, String research) {
        String gate = research(research);
        Map<Integer, EnchantmentData> levels = existing(id);
        levels.replaceAll(
            (level, old) -> new EnchantmentData(
                old.texture.toString(),
                old.vanilla,
                OsmoticRecipes.copy(old.aspects),
                gate));
        change(id, levels);
    }

    @ZenMethod
    public static void setIcon(int id, String icon) {
        String texture = icon(icon);
        Map<Integer, EnchantmentData> levels = existing(id);
        levels.replaceAll(
            (level, old) -> new EnchantmentData(texture, old.vanilla, OsmoticRecipes.copy(old.aspects), old.research));
        change(id, levels);
    }

    @ZenMethod
    public static void removeEnchantment(int id) {
        require(id);
        change(id, null);
    }

    private static Enchantment require(int id) {
        Enchantment enchant = OsmoticRecipes.enchantment(id);
        if (enchant == null) throw new IllegalArgumentException("Unknown enchantment ID: " + id);
        if (enchant.getMinLevel() != 1 || enchant.getMaxLevel() < 1 || enchant.getMaxLevel() > 32767)
            throw new IllegalArgumentException(
                "Osmotic enchantments must have levels starting at 1 and ending at most at 32767");
        return enchant;
    }

    private static Map<Integer, EnchantmentData> existing(int id) {
        require(id);
        Map<Integer, EnchantmentData> old = EnchantmentManager.enchantmentData.get(id);
        if (old == null) throw new IllegalArgumentException(
            "Enchantment " + id + " has no Osmotic recipe; use setEnchantment first");
        return snapshot(old);
    }

    private static String icon(String icon) {
        if (icon == null || !icon.matches("[a-z0-9_.-]+:[a-zA-Z0-9_./-]+\\.png") || icon.contains(".."))
            throw new IllegalArgumentException(
                "Osmotic icons need a texture path such as mypack:textures/enchantments/icon.png");
        return new ResourceLocation(icon).toString();
    }

    private static String research(String research) {
        if (research == null || !research.equals(research.trim()))
            throw new IllegalArgumentException("Research must be a key or an empty string");
        return research;
    }

    private static AspectList cost(String vis) {
        if (vis == null) throw new IllegalArgumentException("Vis cost cannot be null");
        AspectList cost = new AspectList();
        if (vis.trim()
            .isEmpty()) return cost;
        for (String part : vis.split(",", -1)) {
            String[] words = part.trim()
                .split("\\s+");
            if (words.length != 2) throw new IllegalArgumentException("Invalid Osmotic vis cost: " + part);
            Aspect aspect = Aspect.getAspect(words[0]);
            int amount = Integer.parseInt(words[1]);
            if (!OsmoticRecipes.standardPrimal(aspect) || amount <= 0
                || amount > 1000000
                || cost.getAmount(aspect) > 1000000 - amount)
                throw new IllegalArgumentException(
                    "Osmotic costs require the six standard primal aspects and positive amounts up to 1000000");
            cost.add(aspect, amount);
        }
        return cost;
    }

    private static Map<Integer, EnchantmentData> snapshot(Map<Integer, EnchantmentData> source) {
        if (source == null) return null;
        Map<Integer, EnchantmentData> result = new LinkedHashMap<>();
        source.forEach(
            (level, data) -> result.put(
                level,
                new EnchantmentData(
                    data.texture.toString(),
                    data.vanilla,
                    OsmoticRecipes.copy(data.aspects),
                    data.research)));
        return result;
    }

    private static void change(int id, Map<Integer, EnchantmentData> replacement) {
        MineTweakerAPI.apply(new IUndoableAction() {

            private Map<Integer, EnchantmentData> before;

            public void apply() {
                before = snapshot(EnchantmentManager.enchantmentData.get(id));
                put(replacement);
            }

            private void put(Map<Integer, EnchantmentData> data) {
                if (data == null) EnchantmentManager.enchantmentData.remove(id);
                else EnchantmentManager.enchantmentData.put(id, snapshot(data));
            }

            public boolean canUndo() {
                return true;
            }

            public void undo() {
                put(before);
            }

            public String describe() {
                return "Setting Osmotic enchantment " + id;
            }

            public String describeUndo() {
                return "Restoring Osmotic enchantment " + id;
            }

            public Object getOverrideKey() {
                return null;
            }
        });
    }
}
