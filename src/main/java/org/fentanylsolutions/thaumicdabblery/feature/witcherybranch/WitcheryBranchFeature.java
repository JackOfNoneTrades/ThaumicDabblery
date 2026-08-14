package org.fentanylsolutions.thaumicdabblery.feature.witcherybranch;

import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.thaumicdabblery.feature.Feature;
import org.fentanylsolutions.thaumicdabblery.feature.FeatureConfig;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;

public final class WitcheryBranchFeature implements Feature {

    public static final String ID = "mysticBranchSelfInfusion";
    public static final int INFUSION_ID = 11;

    private static volatile boolean enabled = true;
    private static boolean networkInitialized;

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean areRequiredModsLoaded() {
        return Loader.isModLoaded("witchery") && Loader.isModLoaded("ThaumicHorizons");
    }

    public static boolean isActive() {
        return enabled && areRequiredModsLoaded();
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
            "Adds a Thaumic Horizons self-infusion that lets its owner cast with a virtual Witchery Mystic Branch.");
    }

    @Override
    public void init(FMLInitializationEvent event) {
        if (areRequiredModsLoaded() && !networkInitialized) {
            WitcheryBranchNetwork.initialize();
            networkInitialized = true;
        }
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        if (areRequiredModsLoaded()) {
            WitcheryBranchCompat.synchronizeRegistration();
        }
    }

    @Override
    public void onConfigReload() {
        if (areRequiredModsLoaded()) {
            WitcheryBranchCompat.synchronizeRegistration();
        }
    }

    public static void registerClientHandler() {
        if (areRequiredModsLoaded()) {
            WitcheryBranchKeyHandler.register();
        }
    }
}
