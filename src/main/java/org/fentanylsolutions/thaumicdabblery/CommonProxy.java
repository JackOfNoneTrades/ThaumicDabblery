package org.fentanylsolutions.thaumicdabblery;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.ThaumicHorizonsSelfInfusionZen;
import org.fentanylsolutions.thaumicdabblery.feature.FeatureManager;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        FeatureManager.bootstrap();
        Config.synchronizeConfiguration(ThaumicDabblery.configFile);
        FeatureManager.preInit(event);
        ThaumicDabblery.LOG.info(
            "I am {} at version {} with {} feature modules",
            ThaumicDabblery.MODNAME,
            Tags.VERSION,
            FeatureManager.size());
    }

    public void init(FMLInitializationEvent event) {
        FeatureManager.init(event);
        if (Loader.isModLoaded("MineTweaker3") && Loader.isModLoaded("modtweaker2")
            && Loader.isModLoaded("ThaumicHorizons")) {
            ThaumicHorizonsSelfInfusionZen.register();
        }
    }

    public void postInit(FMLPostInitializationEvent event) {
        FeatureManager.postInit(event);
        if (Loader.isModLoaded("MineTweaker3") && Loader.isModLoaded("modtweaker2")
            && Loader.isModLoaded("ThaumicHorizons")) {
            ThaumicHorizonsSelfInfusionZen.initializeDisplayPages();
        }
    }

    public void onConfigReload() {
        Config.synchronizeConfiguration(ThaumicDabblery.configFile);
        FeatureManager.onConfigReload();
    }
}
