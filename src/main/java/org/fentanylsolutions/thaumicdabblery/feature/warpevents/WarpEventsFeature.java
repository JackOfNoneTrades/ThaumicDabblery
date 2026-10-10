package org.fentanylsolutions.thaumicdabblery.feature.warpevents;

import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.WarpEventsZen;
import org.fentanylsolutions.thaumicdabblery.feature.Feature;
import org.fentanylsolutions.thaumicdabblery.feature.FeatureConfig;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;

public final class WarpEventsFeature implements Feature {

    private static volatile boolean enabled = true;
    private static volatile double chance = .25;

    public String id() {
        return "customWarpEvents";
    }

    public void configure(Configuration config) {
        enabled = FeatureConfig.getEnabled(config, id(), true, "Allow scripted warp events and their testing command.");
        chance = config.get(
            FeatureConfig.category(id()),
            "customEventChance",
            .25,
            "Chance (0 to 1) to replace a successful native warp event with one eligible custom event. No eligible events leaves native behavior unchanged.",
            0,
            1)
            .setLanguageKey("thaumicdabblery.config.feature.customWarpEvents.chance")
            .getDouble(.25);
        if (!Double.isFinite(chance)) chance = .25;
        chance = Math.max(0, Math.min(1, chance));
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static double chance() {
        return chance;
    }

    public void init(FMLInitializationEvent event) {
        if (Loader.isModLoaded("MineTweaker3") && Loader.isModLoaded("modtweaker2")) WarpEventsZen.init();
    }
}
