package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

import cpw.mods.fml.relauncher.ReflectionHelper;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethodStatic;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;

/** Adds native smelting pages to either ModTweaker fork's existing research namespace. */
@ZenExpansion("mods.thaumcraft.Research")
public final class FurnacePagesZen {

    private static final List<AddFurnacePage> ACTIVE = new ArrayList<>();

    private FurnacePagesZen() {}

    public static void register() {
        MineTweakerAPI.registerClass(FurnacePagesZen.class);
        MineTweakerImplementationAPI.onPostReload(event -> refreshOutputs());
    }

    @ZenMethodStatic
    public static void addFurnacePage(String key, IItemStack input) {
        if (key == null || key.trim()
            .isEmpty()) {
            MineTweakerAPI.logError("Cannot add furnace page: research key must not be empty");
            return;
        }
        ItemStack stack = input == null ? null : MineTweakerMC.getItemStack(input);
        if (stack == null || stack.getItem() == null
            || stack.stackSize != 1
            || stack.getItemDamage() < 0
            || stack.getItemDamage() == 32767) {
            MineTweakerAPI.logError(
                "Cannot add furnace page to " + key + ": input must be one concrete item, without wildcard metadata");
            return;
        }
        MineTweakerAPI.apply(new AddFurnacePage(key, stack.copy()));
    }

    private static void refreshOutputs() {
        for (AddFurnacePage action : ACTIVE) {
            if (!action.isAttached()) continue;
            ItemStack output = FurnaceRecipes.smelting()
                .getSmeltingResult(action.input);
            if (output == null) {
                MineTweakerAPI.logError(
                    "Removing furnace page from " + action.key
                        + ": its input has no smelting recipe after scripts finished");
                action.removePage();
            } else {
                action.page.recipeOutput = output.copy();
            }
        }
        if (!ACTIVE.isEmpty()) clearRecipeLinks();
    }

    private static void clearRecipeLinks() {
        Map<?, ?> cache = ReflectionHelper.getPrivateValue(ThaumcraftApi.class, null, "keyCache");
        cache.clear();
    }

    private static final class AddFurnacePage implements IUndoableAction {

        private final String key;
        private final ItemStack input;
        private ResearchItem research;
        private ResearchPage page;
        private ResearchPage[] originalPages;

        private AddFurnacePage(String key, ItemStack input) {
            this.key = key;
            this.input = input;
        }

        @Override
        public void apply() {
            research = ResearchCategories.getResearch(key);
            if (research == null) {
                MineTweakerAPI.logError("Cannot add furnace page: missing research " + key);
                return;
            }
            ItemStack output = FurnaceRecipes.smelting()
                .getSmeltingResult(input);
            if (output == null) {
                MineTweakerAPI.logError(
                    "Cannot add furnace page to " + key + ": input has no smelting recipe; register the recipe first");
                return;
            }
            originalPages = research.getPages();
            page = new ResearchPage(input.copy());
            page.recipeOutput = output.copy();
            ResearchPage[] pages = originalPages == null ? new ResearchPage[1]
                : Arrays.copyOf(originalPages, originalPages.length + 1);
            pages[pages.length - 1] = page;
            research.setPages(pages);
            ACTIVE.add(this);
            clearRecipeLinks();
        }

        private boolean isAttached() {
            return page != null && ResearchCategories.getResearch(key) == research
                && research.getPages() != null
                && Arrays.asList(research.getPages())
                    .contains(page);
        }

        private void removePage() {
            if (page == null || research.getPages() == null) return;
            List<ResearchPage> pages = new ArrayList<>(Arrays.asList(research.getPages()));
            if (!pages.remove(page)) return;
            // Preserve the exact original array (including null) when no intervening page changes remain.
            if (Arrays.equals(
                pages.toArray(new ResearchPage[0]),
                originalPages == null ? new ResearchPage[0] : originalPages)) {
                research.setPages(originalPages);
            } else {
                research.setPages(pages.toArray(new ResearchPage[0]));
            }
            clearRecipeLinks();
        }

        @Override
        public void undo() {
            removePage();
            ACTIVE.remove(this);
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public String describe() {
            return "Adding furnace research page to " + key;
        }

        @Override
        public String describeUndo() {
            return "Removing furnace research page from " + key;
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }
}
