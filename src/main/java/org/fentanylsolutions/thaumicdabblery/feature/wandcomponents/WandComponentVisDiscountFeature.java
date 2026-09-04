package org.fentanylsolutions.thaumicdabblery.feature.wandcomponents;

import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.WandComponentsZen;
import org.fentanylsolutions.thaumicdabblery.feature.Feature;
import org.fentanylsolutions.thaumicdabblery.feature.FeatureConfig;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;

public final class WandComponentVisDiscountFeature implements Feature {

    public static final String ID = "wandComponentVisDiscount";

    private static volatile boolean enabled = true;

    public static boolean isEnabled() {
        return enabled;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void configure(Configuration configuration) {
        enabled = FeatureConfig.getEnabled(
            configuration,
            ID,
            true,
            "Allows ModTweaker scripts to set per-aspect wand cap and fixed casting item vis discounts.");
    }

    @Override
    public void init(FMLInitializationEvent event) {
        if (Loader.isModLoaded("MineTweaker3") && Loader.isModLoaded("modtweaker2")) {
            WandComponentsZen.register();
        }
    }
}
