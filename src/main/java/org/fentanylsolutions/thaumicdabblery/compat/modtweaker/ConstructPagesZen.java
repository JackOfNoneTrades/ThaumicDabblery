package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.construct.ConstructPage;

import cpw.mods.fml.relauncher.ReflectionHelper;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethodStatic;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;

@ZenExpansion("mods.thaumcraft.Research")
public final class ConstructPagesZen {

    private ConstructPagesZen() {}

    public static void register() {
        MineTweakerAPI.registerClass(ConstructPagesZen.class);
    }

    @ZenMethodStatic
    public static void addConstructPage(String key, IItemStack[][][] layers) {
        addConstructPage(key, layers, null, "");
    }

    @ZenMethodStatic
    public static void addConstructPage(String key, IItemStack[][][] layers, IItemStack activation) {
        addConstructPage(key, layers, activation, "");
    }

    @ZenMethodStatic
    public static void addConstructPage(String key, IItemStack[][][] layers, IItemStack activation, String vis) {
        try {
            if (key == null || key.trim()
                .isEmpty()) throw new IllegalArgumentException("research key must not be empty");
            if (layers == null || layers.length == 0 || layers.length > 16)
                throw new IllegalArgumentException("supply 1 to 16 layers, bottom first");
            int depth = layers[0] == null ? 0 : layers[0].length;
            int width = depth == 0 || layers[0][0] == null ? 0 : layers[0][0].length;
            if (width < 1 || width > 16 || depth < 1 || depth > 16)
                throw new IllegalArgumentException("layer width and depth must be between 1 and 16");
            ItemStack[][][] cells = new ItemStack[layers.length][depth][width];
            boolean populated = false;
            for (int y = 0; y < layers.length; y++) {
                if (layers[y] == null || layers[y].length != depth)
                    throw new IllegalArgumentException("all layers must have the same depth");
                for (int z = 0; z < depth; z++) {
                    if (layers[y][z] == null || layers[y][z].length != width)
                        throw new IllegalArgumentException("all rows must have the same width");
                    for (int x = 0; x < width; x++) {
                        cells[y][z][x] = stack(layers[y][z][x]);
                        populated |= cells[y][z][x] != null;
                    }
                }
            }
            if (!populated) throw new IllegalArgumentException("structure must contain at least one item or block");
            AspectList cost = new AspectList();
            if (vis == null)
                throw new IllegalArgumentException("vis must be a string; use an empty string for no cost");
            if (!vis.trim()
                .isEmpty()) for (String part : vis.split(",", -1)) {
                    String[] words = part.trim()
                        .split("\\s+");
                    if (words.length != 2)
                        throw new IllegalArgumentException("expected vis entries such as 'aer 10, terra 5'");
                    Aspect aspect = Aspect.getAspect(words[0]);
                    int amount = Integer.parseInt(words[1]);
                    if (aspect == null || amount <= 0 || cost.getAmount(aspect) > Integer.MAX_VALUE - amount)
                        throw new IllegalArgumentException(
                            "vis requires known aspects and positive, non-overflowing amounts");
                    cost.add(aspect, amount);
                }
            MineTweakerAPI.apply(new AddPage(key, new ConstructPage(cells, stack(activation), cost)));
        } catch (IllegalArgumentException error) {
            MineTweakerAPI.logError("Cannot add construct page to " + key + ": " + error.getMessage());
        }
    }

    private static ItemStack stack(IItemStack input) {
        if (input == null) return null;
        ItemStack stack = MineTweakerMC.getItemStack(input);
        if (stack == null || stack.getItem() == null || stack.stackSize != 1 || stack.getItemDamage() < 0)
            throw new IllegalArgumentException(
                "cells and activation items must be single valid stacks; use null for empty cells");
        return stack.copy();
    }

    private static void clearRecipeLinks() {
        Map<?, ?> cache = ReflectionHelper.getPrivateValue(ThaumcraftApi.class, null, "keyCache");
        cache.clear();
    }

    private static final class AddPage implements IUndoableAction {

        private final String key;
        private final ConstructPage page;
        private ResearchItem owner;
        private ResearchPage[] original;

        private AddPage(String key, ConstructPage page) {
            this.key = key;
            this.page = page;
        }

        public void apply() {
            owner = ResearchCategories.getResearch(key);
            if (owner == null) {
                MineTweakerAPI.logError("Cannot add construct page: missing research " + key);
                return;
            }
            original = owner.getPages();
            ResearchPage[] pages = original == null ? new ResearchPage[1]
                : Arrays.copyOf(original, original.length + 1);
            pages[pages.length - 1] = page;
            owner.setPages(pages);
            clearRecipeLinks();
        }

        public void undo() {
            if (owner == null || owner.getPages() == null) return;
            List<ResearchPage> pages = new ArrayList<>(Arrays.asList(owner.getPages()));
            if (!pages.remove(page)) return;
            ResearchPage[] remaining = pages.toArray(new ResearchPage[0]);
            owner.setPages(
                Arrays.equals(remaining, original == null ? new ResearchPage[0] : original) ? original : remaining);
            clearRecipeLinks();
        }

        public boolean canUndo() {
            return true;
        }

        public String describe() {
            return "Adding construct page to " + key;
        }

        public String describeUndo() {
            return "Removing construct page from " + key;
        }

        public Object getOverrideKey() {
            return null;
        }
    }
}
