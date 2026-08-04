package org.fentanylsolutions.thaumicdabblery.feature;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

public final class FeatureConfig {

    private static final String CATEGORY_PREFIX = "features.";

    private FeatureConfig() {}

    public static boolean getEnabled(Configuration configuration, String featureId, boolean defaultValue,
        String comment) {
        String category = category(featureId);
        String languageKey = ThaumicDabblery.MODID + ".config.feature." + featureId;
        configuration.setCategoryLanguageKey(category, languageKey);

        Property property = configuration.get(category, "enabled", defaultValue, comment);
        property.setLanguageKey(languageKey + ".enabled");
        return property.getBoolean(defaultValue);
    }

    public static String category(String featureId) {
        return CATEGORY_PREFIX + featureId;
    }
}
