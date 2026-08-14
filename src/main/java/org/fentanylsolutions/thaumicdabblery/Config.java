package org.fentanylsolutions.thaumicdabblery;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.fentanylsolutions.thaumicdabblery.feature.FeatureManager;

public final class Config {

    private static Configuration configuration;

    public static boolean debugMode;

    public static final class Categories {

        public static final String DEBUG = "debug";

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

        FeatureManager.configure(configuration);

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public static Configuration getRawConfig() {
        return configuration;
    }
}
