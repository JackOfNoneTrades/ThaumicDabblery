package org.fentanylsolutions.thaumicdabblery.feature.researchscangates;

import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.ResearchScanGatesZen;
import org.fentanylsolutions.thaumicdabblery.feature.Feature;
import org.fentanylsolutions.thaumicdabblery.feature.FeatureConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;

public final class ResearchScanGatesFeature implements Feature {

    public static final String ID = "researchScanGates";

    private static volatile boolean enabled = true;
    private static boolean playerHandlerRegistered;

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
            "Allows ModTweaker scripts to reveal research after one or more item and entity scans.");
    }

    @Override
    public void init(FMLInitializationEvent event) {
        if (!playerHandlerRegistered) {
            FMLCommonHandler.instance()
                .bus()
                .register(ScanGatePlayerHandler.INSTANCE);
            playerHandlerRegistered = true;
        }

        if (Loader.isModLoaded("MineTweaker3") && Loader.isModLoaded("modtweaker2")) {
            ResearchScanGatesZen.register();
        }
    }

    @Override
    public void onConfigReload() {
        ScanGateRegistry.synchronizeActivation(enabled);
    }
}
