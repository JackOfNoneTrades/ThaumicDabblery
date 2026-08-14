package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

import com.kentington.thaumichorizons.common.ThaumicHorizons;
import com.kentington.thaumichorizons.common.lib.SelfInfusionRecipe;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IItemStack;
import modtweaker2.helpers.InputHelper;
import modtweaker2.mods.thaumcraft.ThaumcraftHelper;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;

@ZenClass("mods.thaumichorizons.SelfInfusion")
public final class ThaumicHorizonsSelfInfusionZen {

    private static final List<DisplaySlot> DISPLAY_SLOTS = new ArrayList<>();
    private static boolean displaySlotsInitialized;

    private ThaumicHorizonsSelfInfusionZen() {}

    public static void register() {
        MineTweakerAPI.registerClass(ThaumicHorizonsSelfInfusionZen.class);
    }

    public static void initializeDisplayPages() {
        ensureDisplaySlotsInitialized();
    }

    @ZenMethod
    public static void addRecipe(String researchKey, int infusionId, int instability, String aspects,
        IItemStack[] components) {
        ensureDisplaySlotsInitialized();
        String internalResearchKey = requireResearchKey(researchKey);
        if (infusionId <= 0) {
            throw new IllegalArgumentException("Thaumic Horizons self-infusion ID must be positive");
        }
        if (instability < 0) {
            throw new IllegalArgumentException("Thaumic Horizons self-infusion instability cannot be negative");
        }

        SelfInfusionRecipe recipe = new SelfInfusionRecipe(
            internalResearchKey,
            instability,
            requireAspects(aspects),
            requireComponents(components),
            infusionId);
        MineTweakerAPI.apply(new AddAction(requireRegistry(), recipe));
    }

    @ZenMethod
    public static void removeRecipe(int infusionId) {
        ensureDisplaySlotsInitialized();
        List<SelfInfusionRecipe> registry = requireRegistry();
        boolean found = false;
        for (SelfInfusionRecipe recipe : registry) {
            if (recipe.getID() == infusionId) {
                found = true;
                break;
            }
        }
        if (!found) {
            MineTweakerAPI.logWarning(
                "No Thaumic Horizons self-infusion recipe found for infusion ID " + infusionId + ". Command ignored!");
            return;
        }
        MineTweakerAPI.apply(new RemoveAction(registry, infusionId));
    }

    private static String requireResearchKey(String researchKey) {
        if (researchKey == null) {
            throw new IllegalArgumentException("Thaumic Horizons self-infusion research key cannot be null");
        }
        return researchKey.trim();
    }

    private static AspectList requireAspects(String aspects) {
        if (aspects == null) {
            throw new IllegalArgumentException("Thaumic Horizons self-infusion aspects cannot be null");
        }
        try {
            return ThaumcraftHelper.parseAspects(aspects);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid Thaumcraft aspect list: " + aspects, exception);
        }
    }

    private static ItemStack[] requireComponents(IItemStack[] components) {
        if (components == null) {
            throw new IllegalArgumentException("Thaumic Horizons self-infusion components cannot be null");
        }

        ItemStack[] internal = InputHelper.toStacks(components);
        for (int index = 0; index < internal.length; index++) {
            if (internal[index] == null || internal[index].getItem() == null) {
                throw new IllegalArgumentException(
                    "Thaumic Horizons self-infusion component at index " + index + " cannot be null");
            }
            internal[index] = internal[index].copy();
            internal[index].stackSize = 1;
        }
        return internal;
    }

    private static List<SelfInfusionRecipe> requireRegistry() {
        if (ThaumicHorizons.selfRecipes == null) {
            throw new IllegalStateException("Thaumic Horizons self-infusion recipe registry is unavailable");
        }
        return ThaumicHorizons.selfRecipes;
    }

    private static void initializeDisplaySlots() {
        DISPLAY_SLOTS.clear();
        List<SelfInfusionRecipe> registry = requireRegistry();
        Set<ResearchPage> visitedPages = Collections.newSetFromMap(new IdentityHashMap<ResearchPage, Boolean>());
        Set<SelfInfusionRecipe> claimedRecipes = Collections
            .newSetFromMap(new IdentityHashMap<SelfInfusionRecipe, Boolean>());
        for (ResearchCategoryList category : ResearchCategories.researchCategories.values()) {
            for (ResearchItem research : category.research.values()) {
                if (research == null || research.getPages() == null) {
                    continue;
                }

                ResearchPage[] pages = research.getPages();
                for (int index = 0; index < pages.length; index++) {
                    ResearchPage page = pages[index];
                    if (!visitedPages.add(page)) {
                        continue;
                    }
                    InfusionRecipe[] displayRecipes = getInfusionRecipes(page);
                    if (displayRecipes.length == 0) {
                        continue;
                    }

                    DisplaySlot slot = new DisplaySlot(research, index, page);
                    for (InfusionRecipe displayRecipe : displayRecipes) {
                        SelfInfusionRecipe matchedRecipe = findMatchingRecipe(registry, displayRecipe, claimedRecipes);
                        if (matchedRecipe != null) {
                            slot.addTemplate(matchedRecipe.getID(), displayRecipe);
                            claimedRecipes.add(matchedRecipe);
                        }
                    }
                    if (!slot.templates.isEmpty()) {
                        DISPLAY_SLOTS.add(slot);
                    }
                }
            }
        }
        linkUniqueResearchPages(registry, claimedRecipes);
        int linkedRecipes = 0;
        for (DisplaySlot slot : DISPLAY_SLOTS) {
            linkedRecipes += slot.templates.size();
        }
        ThaumicDabblery.LOG.info(
            "Linked {} Thaumic Horizons self-infusion recipes to their Thaumonomicon display pages",
            linkedRecipes);
        displaySlotsInitialized = true;
    }

    private static void linkUniqueResearchPages(List<SelfInfusionRecipe> registry,
        Set<SelfInfusionRecipe> claimedRecipes) {
        Set<ResearchPage> linkedPages = Collections.newSetFromMap(new IdentityHashMap<ResearchPage, Boolean>());
        for (DisplaySlot slot : DISPLAY_SLOTS) {
            linkedPages.add(slot.currentPage);
        }
        for (SelfInfusionRecipe recipe : registry) {
            if (claimedRecipes.contains(recipe) || countRecipesForResearch(registry, recipe.getResearch()) != 1) {
                continue;
            }
            ResearchItem research = ResearchCategories.getResearch(recipe.getResearch());
            if (research == null || research.getPages() == null) {
                continue;
            }
            ResearchPage matchedPage = null;
            int matchedIndex = -1;
            InfusionRecipe matchedDisplayRecipe = null;
            ResearchPage[] pages = research.getPages();
            for (int index = 0; index < pages.length; index++) {
                InfusionRecipe[] displayRecipes = getInfusionRecipes(pages[index]);
                if (!linkedPages.contains(pages[index]) && displayRecipes.length == 1) {
                    if (matchedPage != null) {
                        matchedPage = null;
                        break;
                    }
                    matchedPage = pages[index];
                    matchedIndex = index;
                    matchedDisplayRecipe = displayRecipes[0];
                }
            }
            if (matchedPage != null) {
                DisplaySlot slot = new DisplaySlot(research, matchedIndex, matchedPage);
                slot.addTemplate(recipe.getID(), matchedDisplayRecipe);
                DISPLAY_SLOTS.add(slot);
                linkedPages.add(matchedPage);
                claimedRecipes.add(recipe);
            }
        }
    }

    private static int countRecipesForResearch(List<SelfInfusionRecipe> registry, String researchKey) {
        int count = 0;
        for (SelfInfusionRecipe recipe : registry) {
            if (researchKey.equals(recipe.getResearch())) {
                count++;
            }
        }
        return count;
    }

    private static void ensureDisplaySlotsInitialized() {
        if (!displaySlotsInitialized) {
            initializeDisplaySlots();
        }
    }

    private static InfusionRecipe[] getInfusionRecipes(ResearchPage page) {
        if (page != null && page.recipe instanceof InfusionRecipe) {
            return new InfusionRecipe[] { (InfusionRecipe) page.recipe };
        }
        if (page != null && page.recipe instanceof InfusionRecipe[]) {
            return (InfusionRecipe[]) page.recipe;
        }
        return new InfusionRecipe[0];
    }

    private static SelfInfusionRecipe findMatchingRecipe(List<SelfInfusionRecipe> registry,
        InfusionRecipe displayRecipe, Set<SelfInfusionRecipe> claimedRecipes) {
        for (SelfInfusionRecipe recipe : registry) {
            if (!claimedRecipes.contains(recipe) && recipe.getResearch()
                .equals(displayRecipe.getResearch()) && recipesMatch(recipe, displayRecipe)) {
                return recipe;
            }
        }
        for (SelfInfusionRecipe recipe : registry) {
            if (!claimedRecipes.contains(recipe) && recipesMatch(recipe, displayRecipe)) {
                return recipe;
            }
        }
        return null;
    }

    private static boolean recipesMatch(SelfInfusionRecipe selfRecipe, InfusionRecipe displayRecipe) {
        return selfRecipe.getInstability() == displayRecipe.getInstability()
            && aspectListsMatch(selfRecipe.getAspects(), displayRecipe.getAspects())
            && componentsMatch(selfRecipe.getComponents(), displayRecipe.getComponents());
    }

    private static boolean aspectListsMatch(AspectList first, AspectList second) {
        Aspect[] firstAspects = first == null ? null : first.getAspects();
        Aspect[] secondAspects = second == null ? null : second.getAspects();
        int firstLength = firstAspects == null ? 0 : firstAspects.length;
        int secondLength = secondAspects == null ? 0 : secondAspects.length;
        if (firstLength != secondLength) {
            return false;
        }
        for (int index = 0; index < firstLength; index++) {
            Aspect aspect = firstAspects[index];
            if (first.getAmount(aspect) != second.getAmount(aspect)) {
                return false;
            }
        }
        return true;
    }

    private static boolean componentsMatch(ItemStack[] first, ItemStack[] second) {
        if (first == null || second == null || first.length != second.length) {
            return false;
        }
        boolean[] matched = new boolean[second.length];
        for (ItemStack firstStack : first) {
            boolean found = false;
            for (int index = 0; index < second.length; index++) {
                if (!matched[index] && SelfInfusionRecipe.areItemStacksEqual(firstStack, second[index], true)) {
                    matched[index] = true;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static void refreshDisplay(int infusionId) {
        int refreshedPages = 0;
        for (DisplaySlot slot : DISPLAY_SLOTS) {
            if (slot.contains(infusionId)) {
                slot.refresh(requireRegistry());
                refreshedPages++;
            }
        }
        ThaumicDabblery.LOG
            .debug("Synchronized {} Thaumonomicon display pages for self-infusion ID {}", refreshedPages, infusionId);
    }

    private static final class AddAction implements IUndoableAction {

        private final List<SelfInfusionRecipe> registry;
        private final SelfInfusionRecipe recipe;
        private boolean added;

        private AddAction(List<SelfInfusionRecipe> registry, SelfInfusionRecipe recipe) {
            this.registry = registry;
            this.recipe = recipe;
        }

        @Override
        public void apply() {
            added = registry.add(recipe);
            refreshDisplay(recipe.getID());
        }

        @Override
        public boolean canUndo() {
            return added;
        }

        @Override
        public void undo() {
            registry.remove(recipe);
            refreshDisplay(recipe.getID());
        }

        @Override
        public String describe() {
            return "Adding Thaumic Horizons self-infusion recipe for infusion ID " + recipe.getID();
        }

        @Override
        public String describeUndo() {
            return "Removing added Thaumic Horizons self-infusion recipe for infusion ID " + recipe.getID();
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }

    private static final class RemoveAction implements IUndoableAction {

        private final List<SelfInfusionRecipe> registry;
        private final int infusionId;
        private final List<RemovedRecipe> removed = new ArrayList<>();

        private RemoveAction(List<SelfInfusionRecipe> registry, int infusionId) {
            this.registry = registry;
            this.infusionId = infusionId;
        }

        @Override
        public void apply() {
            removed.clear();
            for (int index = registry.size() - 1; index >= 0; index--) {
                SelfInfusionRecipe recipe = registry.get(index);
                if (recipe.getID() == infusionId) {
                    removed.add(new RemovedRecipe(index, recipe));
                    registry.remove(index);
                }
            }
            refreshDisplay(infusionId);
        }

        @Override
        public boolean canUndo() {
            return !removed.isEmpty();
        }

        @Override
        public void undo() {
            for (int index = removed.size() - 1; index >= 0; index--) {
                RemovedRecipe entry = removed.get(index);
                registry.add(Math.min(entry.index, registry.size()), entry.recipe);
            }
            refreshDisplay(infusionId);
        }

        @Override
        public String describe() {
            return "Removing Thaumic Horizons self-infusion recipes for infusion ID " + infusionId;
        }

        @Override
        public String describeUndo() {
            return "Restoring Thaumic Horizons self-infusion recipes for infusion ID " + infusionId;
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }

    private static final class RemovedRecipe {

        private final int index;
        private final SelfInfusionRecipe recipe;

        private RemovedRecipe(int index, SelfInfusionRecipe recipe) {
            this.index = index;
            this.recipe = recipe;
        }
    }

    private static final class DisplaySlot {

        private final ResearchItem research;
        private final int originalIndex;
        private final List<DisplayTemplate> templates = new ArrayList<>();
        private ResearchPage currentPage;

        private DisplaySlot(ResearchItem research, int originalIndex, ResearchPage currentPage) {
            this.research = research;
            this.originalIndex = originalIndex;
            this.currentPage = currentPage;
        }

        private void addTemplate(int infusionId, InfusionRecipe displayRecipe) {
            templates.add(new DisplayTemplate(infusionId, displayRecipe));
        }

        private boolean contains(int infusionId) {
            for (DisplayTemplate template : templates) {
                if (template.infusionId == infusionId) {
                    return true;
                }
            }
            return false;
        }

        private void refresh(List<SelfInfusionRecipe> registry) {
            ResearchPage[] pages = research.getPages();
            int currentIndex = findPage(pages, currentPage);
            ResearchPage replacement = createPage(registry);
            if (replacement == null) {
                if (currentIndex >= 0) {
                    research.setPages(removePage(pages, currentIndex));
                }
                currentPage = null;
                return;
            }

            if (currentIndex >= 0) {
                pages[currentIndex] = replacement;
                research.setPages(pages);
            } else {
                research.setPages(insertPage(pages, Math.min(originalIndex, pages.length), replacement));
            }
            currentPage = replacement;
        }

        private ResearchPage createPage(List<SelfInfusionRecipe> registry) {
            List<InfusionRecipe> displayRecipes = new ArrayList<>();
            for (DisplayTemplate template : templates) {
                for (SelfInfusionRecipe recipe : registry) {
                    if (recipe.getID() == template.infusionId) {
                        displayRecipes.add(template.createRecipe(recipe));
                    }
                }
            }
            if (displayRecipes.isEmpty()) {
                return null;
            }
            return displayRecipes.size() == 1 ? new ResearchPage(displayRecipes.get(0))
                : new ResearchPage(displayRecipes.toArray(new InfusionRecipe[0]));
        }

        private static int findPage(ResearchPage[] pages, ResearchPage target) {
            if (pages == null || target == null) {
                return -1;
            }
            for (int index = 0; index < pages.length; index++) {
                if (pages[index] == target) {
                    return index;
                }
            }
            return -1;
        }

        private static ResearchPage[] removePage(ResearchPage[] pages, int removedIndex) {
            ResearchPage[] result = new ResearchPage[pages.length - 1];
            System.arraycopy(pages, 0, result, 0, removedIndex);
            System.arraycopy(pages, removedIndex + 1, result, removedIndex, pages.length - removedIndex - 1);
            return result;
        }

        private static ResearchPage[] insertPage(ResearchPage[] pages, int insertedIndex, ResearchPage page) {
            ResearchPage[] result = new ResearchPage[pages.length + 1];
            System.arraycopy(pages, 0, result, 0, insertedIndex);
            result[insertedIndex] = page;
            System.arraycopy(pages, insertedIndex, result, insertedIndex + 1, pages.length - insertedIndex);
            return result;
        }

        private static Object copyOutput(Object output) {
            return output instanceof ItemStack ? ((ItemStack) output).copy() : output;
        }

        private static ItemStack copyStack(ItemStack stack) {
            return stack == null ? null : stack.copy();
        }

        private static ItemStack[] copyStacks(ItemStack[] stacks) {
            ItemStack[] result = new ItemStack[stacks.length];
            for (int index = 0; index < stacks.length; index++) {
                result[index] = copyStack(stacks[index]);
            }
            return result;
        }
    }

    private static final class DisplayTemplate {

        private final int infusionId;
        private final String displayResearch;
        private final ItemStack recipeInput;
        private final Object recipeOutput;

        private DisplayTemplate(int infusionId, InfusionRecipe displayRecipe) {
            this.infusionId = infusionId;
            displayResearch = displayRecipe.getResearch();
            recipeInput = DisplaySlot.copyStack(displayRecipe.getRecipeInput());
            recipeOutput = DisplaySlot.copyOutput(displayRecipe.getRecipeOutput());
        }

        private InfusionRecipe createRecipe(SelfInfusionRecipe recipe) {
            return new InfusionRecipe(
                displayResearch,
                DisplaySlot.copyOutput(recipeOutput),
                recipe.getInstability(),
                recipe.getAspects(),
                DisplaySlot.copyStack(recipeInput),
                DisplaySlot.copyStacks(recipe.getComponents()));
        }
    }
}
