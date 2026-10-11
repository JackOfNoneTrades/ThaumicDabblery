package org.fentanylsolutions.thaumicdabblery.feature.witcheryoven;

import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.WitcheryOvenZen;
import org.fentanylsolutions.thaumicdabblery.feature.Feature;
import org.fentanylsolutions.thaumicdabblery.feature.FeatureConfig;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public final class WitcheryOvenFeature implements Feature {

    private static boolean enabled = true;

    public String id() {
        return "witcheryOven";
    }

    public void configure(Configuration config) {
        enabled = FeatureConfig.getEnabled(
            config,
            id(),
            true,
            "Allow scripted Witches' Oven recipes. Reload scripts after changing this setting.");
    }

    public static boolean isEnabled() {
        return enabled;
    }

    private boolean available() {
        return Loader.isModLoaded("witchery") && Loader.isModLoaded("MineTweaker3")
            && Loader.isModLoaded("modtweaker2");
    }

    public void init(FMLInitializationEvent event) {
        if (available()) WitcheryOvenZen.init();
    }

    public void serverStarting(FMLServerStartingEvent event) {
        if (available()) WitcheryOvenZen.registerCommand();
    }
}
