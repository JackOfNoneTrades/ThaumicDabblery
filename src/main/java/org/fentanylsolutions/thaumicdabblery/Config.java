package org.fentanylsolutions.thaumicdabblery;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.fentanylsolutions.thaumicdabblery.feature.FeatureManager;

public final class Config {

    private static Configuration configuration;

    public static boolean debugMode;
    public static volatile boolean showVortexCraftingRays = true;
    public static volatile int minimumVisCostPercent = 10;

    public static final class Categories {

        public static final String CLIENT = "client";
        public static final String DEBUG = "debug";
        public static final String VIS_COSTS = "viscosts";

        private Categories() {}
    }

    private Config() {}

    public static void synchronizeConfiguration(File configFile) {
        ThaumicDabblery.debug("Loading config");
        configuration = new Configuration(configFile);

        String debugLanguageKey = ThaumicDabblery.MODID + ".config.debug";
        configuration.setCategoryLanguageKey(Categories.DEBUG, debugLanguageKey);
        Property debugModeProperty = configuration.get(Categories.DEBUG, "debugMode", debugMode, "Enable debug mode.");
        debugModeProperty.setLanguageKey(debugLanguageKey + ".debugMode");
        debugMode = debugModeProperty.getBoolean();

        String visCostsLanguageKey = ThaumicDabblery.MODID + ".config.visCosts";
        configuration.setCategoryLanguageKey(Categories.VIS_COSTS, visCostsLanguageKey);
        Property minimumVisCostProperty = configuration.get(
            Categories.VIS_COSTS,
            "minimumVisCostPercent",
            10,
            "Minimum final Vis cost as a percentage of the base cost, after all discounts. "
                + "Applies to wand casting and crafting, including Thaumic Bases bracelets. "
                + "10 preserves Thaumcraft's default; 0 allows free use with sufficient discounts. "
                + "Use the same value on the client and server.",
            0,
            100);
        minimumVisCostProperty.setLanguageKey(visCostsLanguageKey + ".minimumVisCostPercent");
        minimumVisCostPercent = Math.max(0, Math.min(100, minimumVisCostProperty.getInt(10)));
        minimumVisCostProperty.set(minimumVisCostPercent);

        if (cpw.mods.fml.common.FMLCommonHandler.instance()
            .getSide()
            .isClient()) {
            configuration.setCategoryLanguageKey(Categories.CLIENT, ThaumicDabblery.MODID + ".config.client");
            Property rays = configuration.get(
                Categories.CLIENT,
                "showVortexCraftingRays",
                true,
                "Show rotating rays during Planar Vortex crafting. Client-side visual preference only. "
                    + "Disabling this keeps the contraction, brightness changes and crafting timing.");
            rays.setLanguageKey(ThaumicDabblery.MODID + ".config.client.showVortexCraftingRays");
            showVortexCraftingRays = rays.getBoolean(true);
        }

        FeatureManager.configure(configuration);

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public static Configuration getRawConfig() {
        return configuration;
    }
}
