package org.fentanylsolutions.thaumicdabblery.feature.aurapylon;

import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.AuraPylonZen;
import org.fentanylsolutions.thaumicdabblery.feature.Feature;
import org.fentanylsolutions.thaumicdabblery.feature.FeatureConfig;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;

public final class AuraPylonFeature implements Feature {

    public static final String ID = "auraPylon";
    private static boolean enabled = true;

    public static boolean isEnabled() {
        return enabled;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void configure(Configuration configuration) {
        enabled = FeatureConfig
            .getEnabled(configuration, ID, true, "Allows scripts to customize Gadomancy Aura Pylon effects.");
    }

    @Override
    public void init(FMLInitializationEvent event) {
        if (Loader.isModLoaded("MineTweaker3") && Loader.isModLoaded("modtweaker2")) AuraPylonZen.register();
    }

    @Override
    public void onConfigReload() {
        if (Loader.isModLoaded("gadomancy")) AuraPylonRegistry.refresh();
    }
}
