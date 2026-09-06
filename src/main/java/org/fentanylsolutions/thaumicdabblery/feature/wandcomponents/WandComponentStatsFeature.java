package org.fentanylsolutions.thaumicdabblery.feature.wandcomponents;

import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.thaumicdabblery.feature.Feature;
import org.fentanylsolutions.thaumicdabblery.feature.FeatureConfig;

public final class WandComponentStatsFeature implements Feature {

    private static volatile boolean enabled = true;

    public static boolean isEnabled() {
        return enabled;
    }

    @Override
    public String id() {
        return "wandComponentStats";
    }

    @Override
    public void configure(Configuration configuration) {
        enabled = FeatureConfig.getEnabled(
            configuration,
            id(),
            true,
            "Allows scripts to customize wand assembly costs, capacity, regeneration and innate Potency, including bracelets.");
        WandComponentStatsRegistry.refreshNativeValues();
    }
}
