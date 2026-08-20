package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import net.minecraft.util.ResourceLocation;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;

@ZenClass("mods.thaumcraft.ResearchTabs")
public final class ResearchTabsZen {

    private static final String CATEGORY_TRANSLATION_PREFIX = "tc.research_category.";

    private ResearchTabsZen() {}

    public static void register() {
        MineTweakerAPI.registerClass(ResearchTabsZen.class);
    }

    @ZenMethod
    public static void setName(String categoryKey, String name) {
        String key = requireCategory(categoryKey);
        MineTweakerAPI.game.setLocalization(CATEGORY_TRANSLATION_PREFIX + key, requireText(name, "name"));
    }

    @ZenMethod
    public static void setName(String categoryKey, String language, String name) {
        String key = requireCategory(categoryKey);
        MineTweakerAPI.game.setLocalization(
            requireText(language, "language"),
            CATEGORY_TRANSLATION_PREFIX + key,
            requireText(name, "name"));
    }

    @ZenMethod
    public static void setBackground(String categoryKey, String resourceDomain, String resourcePath) {
        setTexture(categoryKey, resourceDomain, resourcePath, TextureTarget.BACKGROUND);
    }

    @ZenMethod
    public static void setIcon(String categoryKey, String resourceDomain, String resourcePath) {
        setTexture(categoryKey, resourceDomain, resourcePath, TextureTarget.ICON);
    }

    @ZenMethod
    public static void setResearchGate(String categoryKey, String researchKey) {
        String category = requireCategory(categoryKey);
        String research = requireText(researchKey, "research gate");
        if (ResearchCategories.getResearch(research) == null) {
            throw new IllegalArgumentException("Unknown Thaumcraft research: " + research);
        }
        ResearchTabGateRegistry.validateCanSet(category);
        MineTweakerAPI.apply(new SetResearchGateAction(category, research));
    }

    @ZenMethod
    public static void clearResearchGate(String categoryKey) {
        MineTweakerAPI.apply(new ClearResearchGateAction(requireCategory(categoryKey)));
    }

    private static void setTexture(String categoryKey, String resourceDomain, String resourcePath,
        TextureTarget target) {
        String key = requireCategory(categoryKey);
        ResourceLocation texture = new ResourceLocation(
            requireText(resourceDomain, "resource domain"),
            requireText(resourcePath, "resource path"));
        MineTweakerAPI.apply(new SetTextureAction(key, texture, target));
    }

    private static String requireCategory(String categoryKey) {
        String key = requireText(categoryKey, "category key");
        if (ResearchCategories.getResearchList(key) == null) {
            throw new IllegalArgumentException("Unknown Thaumcraft research category: " + key);
        }
        return key;
    }

    private static String requireText(String value, String description) {
        if (value == null || value.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Thaumcraft research tab " + description + " cannot be empty");
        }
        return value.trim();
    }

    private enum TextureTarget {

        BACKGROUND("background"),
        ICON("icon");

        private final String description;

        TextureTarget(String description) {
            this.description = description;
        }

        private ResourceLocation get(ResearchCategoryList category) {
            return this == BACKGROUND ? category.background : category.icon;
        }

        private void set(ResearchCategoryList category, ResourceLocation texture) {
            if (this == BACKGROUND) {
                category.background = texture;
            } else {
                category.icon = texture;
            }
        }
    }

    private static final class SetTextureAction implements IUndoableAction {

        private final String categoryKey;
        private final ResourceLocation texture;
        private final TextureTarget target;

        private ResearchCategoryList category;
        private ResourceLocation original;

        private SetTextureAction(String categoryKey, ResourceLocation texture, TextureTarget target) {
            this.categoryKey = categoryKey;
            this.texture = texture;
            this.target = target;
        }

        @Override
        public void apply() {
            category = ResearchCategories.getResearchList(categoryKey);
            if (category == null) {
                MineTweakerAPI.logError(
                    "Cannot set the " + target.description + " of missing Thaumcraft research category " + categoryKey);
                return;
            }
            original = target.get(category);
            target.set(category, texture);
        }

        @Override
        public boolean canUndo() {
            return category != null;
        }

        @Override
        public void undo() {
            target.set(category, original);
        }

        @Override
        public String describe() {
            return "Setting Thaumcraft research category " + categoryKey + " " + target.description + " to " + texture;
        }

        @Override
        public String describeUndo() {
            return "Restoring Thaumcraft research category " + categoryKey + " " + target.description;
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }

    private abstract static class ResearchGateAction implements IUndoableAction {

        protected final String categoryKey;
        protected ResearchTabGateRegistry.Change change;

        private ResearchGateAction(String categoryKey) {
            this.categoryKey = categoryKey;
        }

        @Override
        public boolean canUndo() {
            return change != null;
        }

        @Override
        public void undo() {
            change.undo();
        }

        @Override
        public String describeUndo() {
            return "Restoring Thaumcraft research category " + categoryKey + " research gate";
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }

    private static final class SetResearchGateAction extends ResearchGateAction {

        private final String researchKey;

        private SetResearchGateAction(String categoryKey, String researchKey) {
            super(categoryKey);
            this.researchKey = researchKey;
        }

        @Override
        public void apply() {
            change = ResearchTabGateRegistry.set(categoryKey, researchKey);
        }

        @Override
        public String describe() {
            return "Hiding Thaumcraft research category " + categoryKey + " until " + researchKey + " is complete";
        }
    }

    private static final class ClearResearchGateAction extends ResearchGateAction {

        private ClearResearchGateAction(String categoryKey) {
            super(categoryKey);
        }

        @Override
        public void apply() {
            change = ResearchTabGateRegistry.clear(categoryKey);
        }

        @Override
        public String describe() {
            return "Clearing Thaumcraft research category " + categoryKey + " research gate";
        }
    }
}
