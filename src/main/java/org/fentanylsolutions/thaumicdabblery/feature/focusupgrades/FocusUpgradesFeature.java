package org.fentanylsolutions.thaumicdabblery.feature.focusupgrades;

import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.FocusUpgradeCommand;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.FocusUpgradesZen;
import org.fentanylsolutions.thaumicdabblery.feature.Feature;
import org.fentanylsolutions.thaumicdabblery.feature.FeatureConfig;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public final class FocusUpgradesFeature implements Feature {

    public static final String ID = "focusUpgrades";

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
            "Allows ModTweaker scripts to customize Focal Manipulator upgrade choices.");
    }

    @Override
    public void init(FMLInitializationEvent event) {
        if (Loader.isModLoaded("MineTweaker3") && Loader.isModLoaded("modtweaker2")) {
            FocusUpgradesZen.register();
        }
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        if (Loader.isModLoaded("MineTweaker3") && Loader.isModLoaded("modtweaker2")) {
            FocusUpgradeCommand.register();
        }
    }
}
