package org.fentanylsolutions.thaumicdabblery.feature.witcherybranch;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

import com.emoniph.witchery.Witchery;
import com.kentington.thaumichorizons.common.ThaumicHorizons;
import com.kentington.thaumichorizons.common.lib.SelfInfusionRecipe;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;

final class WitcheryBranchCompat {

    private static final String RESEARCH_KEY = "thaumicdabbleryMysticBranch";
    private static final String RESEARCH_CATEGORY = "ThaumicHorizons";
    private static final String PARENT_RESEARCH_KEY = "humanInfusion";
    private static final String LORE_PAGE_ONE_KEY = "thaumicdabblery.research.mysticBranch.page1";
    private static final String LORE_PAGE_TWO_KEY = "thaumicdabblery.research.mysticBranch.page2";
    private static final int INSTABILITY = 10;
    private static final int RESEARCH_COLUMN = 19;
    private static final int RESEARCH_ROW = 5;

    private static SelfInfusionRecipe recipe;
    private static ResearchPage lorePageOne;
    private static ResearchPage lorePageTwo;
    private static ResearchPage researchPage;
    private static ResearchItem research;
    private static boolean registered;

    private WitcheryBranchCompat() {}

    static synchronized boolean isRegistered() {
        return registered;
    }

    static synchronized void synchronizeRegistration() {
        if (WitcheryBranchFeature.isEnabled()) {
            register();
        } else {
            unregister();
        }
    }

    private static void register() {
        if (registered) {
            return;
        }

        if (ThaumicHorizons.selfRecipes == null) {
            ThaumicDabblery.LOG.error("Could not register Mystic Branch self-infusion: recipe registry is unavailable");
            return;
        }

        for (SelfInfusionRecipe existing : ThaumicHorizons.selfRecipes) {
            if (existing.getID() == WitcheryBranchFeature.INFUSION_ID && existing != recipe) {
                ThaumicDabblery.LOG.error(
                    "Could not register Mystic Branch self-infusion: Thaumic Horizons infusion ID {} is already used",
                    WitcheryBranchFeature.INFUSION_ID);
                return;
            }
        }

        ensureObjectsCreated();
        ResearchItem existingResearch = ResearchCategories.getResearch(RESEARCH_KEY);
        if (existingResearch != null && existingResearch != research) {
            ThaumicDabblery.LOG
                .error("Could not register Mystic Branch self-infusion: research key {} is already used", RESEARCH_KEY);
            return;
        }
        if (!ThaumicHorizons.selfRecipes.contains(recipe)) {
            ThaumicHorizons.selfRecipes.add(recipe);
        }
        if (existingResearch == null) {
            ResearchCategories.addResearch(research);
        }
        if (ResearchCategories.getResearch(RESEARCH_KEY) != research) {
            ThaumicDabblery.LOG.error(
                "Could not register Mystic Branch self-infusion research at column {}, row {}",
                RESEARCH_COLUMN,
                RESEARCH_ROW);
            ThaumicHorizons.selfRecipes.remove(recipe);
            return;
        }
        registered = true;
        ThaumicDabblery.LOG.info(
            "Registered Mystic Branch self-infusion with Thaumic Horizons infusion ID {}",
            WitcheryBranchFeature.INFUSION_ID);
    }

    private static void unregister() {
        if (ThaumicHorizons.selfRecipes != null && recipe != null) {
            ThaumicHorizons.selfRecipes.remove(recipe);
        }
        removeResearch();
        registered = false;
    }

    private static void ensureObjectsCreated() {
        if (recipe == null) {
            recipe = new SelfInfusionRecipe(
                RESEARCH_KEY,
                INSTABILITY,
                createEssentiaCost(),
                createComponents(),
                WitcheryBranchFeature.INFUSION_ID);
        }
        if (lorePageOne == null) {
            lorePageOne = new ResearchPage(LORE_PAGE_ONE_KEY);
        }
        if (lorePageTwo == null) {
            lorePageTwo = new ResearchPage(LORE_PAGE_TWO_KEY);
        }
        if (researchPage == null) {
            ItemStack displayOutput = new ItemStack(
                ThaumicHorizons.itemInfusionSelfCheat,
                1,
                WitcheryBranchFeature.INFUSION_ID);

            InfusionRecipe displayRecipe = new InfusionRecipe(
                RESEARCH_KEY,
                displayOutput,
                INSTABILITY,
                createEssentiaCost(),
                new ItemStack(ThaumicHorizons.itemDummy, 1, 15),
                createComponents());
            researchPage = new ResearchPage(displayRecipe);
        }
        if (research == null) {
            research = new ResearchItem(
                RESEARCH_KEY,
                RESEARCH_CATEGORY,
                new AspectList().add(Aspect.FLESH, 3)
                    .add(Aspect.SOUL, 3)
                    .add(Aspect.MAGIC, 3),
                RESEARCH_COLUMN,
                RESEARCH_ROW,
                3,
                new ItemStack(ThaumicHorizons.itemInfusionSelfCheat, 1, WitcheryBranchFeature.INFUSION_ID));
            research.setParents(PARENT_RESEARCH_KEY);
            research.setConcealed();
            research.setHidden();
            research.setItemTriggers(new ItemStack(Witchery.Items.MYSTIC_BRANCH));
            research.setPages(lorePageOne, lorePageTwo, researchPage);
        }
    }

    private static AspectList createEssentiaCost() {
        return new AspectList().add(Aspect.FLESH, 24)
            .add(Aspect.SOUL, 32)
            .add(Aspect.MAN, 16)
            .add(Aspect.MOTION, 16)
            .add(Aspect.MAGIC, 64);
    }

    private static ItemStack[] createComponents() {
        return new ItemStack[] { Witchery.Items.GENERIC.itemMysticUnguent.createStack(),
            new ItemStack(Witchery.Items.WITCH_HAND), Witchery.Items.GENERIC.itemGoldenThread.createStack(),
            new ItemStack(Witchery.Items.WITCH_HAND) };
    }

    private static void removeResearch() {
        if (research == null) {
            return;
        }
        ResearchCategoryList category = ResearchCategories.getResearchList(RESEARCH_CATEGORY);
        if (category != null && category.research.get(RESEARCH_KEY) == research) {
            category.research.remove(RESEARCH_KEY);
        }
    }
}
