package org.fentanylsolutions.thaumicdabblery;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.thaumicdabblery.feature.FeatureManager;

public final class Config {

    private static Configuration configuration;

    private Config() {}

    public static void synchronizeConfiguration(File configFile) {
        configuration = new Configuration(configFile);
        FeatureManager.configure(configuration);

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public static Configuration getRawConfig() {
        return configuration;
    }
}
