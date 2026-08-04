package org.fentanylsolutions.thaumicdabblery.feature.visdiscount;

import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.VisDiscountZen;
import org.fentanylsolutions.thaumicdabblery.feature.Feature;
import org.fentanylsolutions.thaumicdabblery.feature.FeatureConfig;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;

public final class VisDiscountFeature implements Feature {

    public static final String ID = "visDiscount";

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
            "Allows ModTweaker scripts to set the vis discount of equipped armor and Baubles.");
    }

    @Override
    public void init(FMLInitializationEvent event) {
        if (Loader.isModLoaded("MineTweaker3") && Loader.isModLoaded("modtweaker2")) {
            VisDiscountZen.register();
        }
    }
}
