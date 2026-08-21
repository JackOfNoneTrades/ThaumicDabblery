package org.fentanylsolutions.thaumicdabblery.feature.itemstats;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.RunicShieldingZen;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.WarpingGearZen;
import org.fentanylsolutions.thaumicdabblery.feature.Feature;
import org.fentanylsolutions.thaumicdabblery.feature.FeatureConfig;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;

public final class ThaumcraftItemStatsFeature implements Feature {

    public static final String ID = "thaumcraftItemStats";

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
            "Allows ModTweaker scripts to set equipped item warp and runic shielding properties.");
    }

    @Override
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(WarpingGearTooltipHandler.INSTANCE);
        if (Loader.isModLoaded("MineTweaker3") && Loader.isModLoaded("modtweaker2")) {
            WarpingGearZen.register();
            RunicShieldingZen.register();
        }
    }
}
