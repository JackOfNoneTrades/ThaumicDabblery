package org.fentanylsolutions.thaumicdabblery.feature.witcheryrites;

import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.WitcheryRitesZen;
import org.fentanylsolutions.thaumicdabblery.feature.Feature;
import org.fentanylsolutions.thaumicdabblery.feature.FeatureConfig;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public final class WitcheryRitesFeature implements Feature {

    private static boolean enabled = true;

    public String id() {
        return "witcheryRites";
    }

    public void configure(Configuration config) {
        enabled = FeatureConfig.getEnabled(
            config,
            id(),
            true,
            "Allow scripts to edit existing Witchery rite requirements. Reload scripts after changing this setting.");
    }

    public static boolean isEnabled() {
        return enabled;
    }

    private boolean available() {
        return Loader.isModLoaded("witchery") && Loader.isModLoaded("MineTweaker3")
            && Loader.isModLoaded("modtweaker2");
    }

    public void init(FMLInitializationEvent event) {
        if (available()) WitcheryRitesZen.init();
    }

    public void serverStarting(FMLServerStartingEvent event) {
        if (available()) WitcheryRitesZen.registerCommand();
    }
}
