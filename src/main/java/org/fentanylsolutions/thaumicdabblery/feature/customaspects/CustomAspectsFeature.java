package org.fentanylsolutions.thaumicdabblery.feature.customaspects;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.CustomAspectScriptLoader;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.CustomAspectsZen;
import org.fentanylsolutions.thaumicdabblery.feature.Feature;
import org.fentanylsolutions.thaumicdabblery.feature.FeatureConfig;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;

public final class CustomAspectsFeature implements Feature {

    public static final String ID = "customAspects";
    private static final String COMMENT = "Register compound aspects from config/thaumicdabblery/aspects/*.zs at startup. Requires a full restart.";

    private boolean enabled;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void configure(Configuration configuration) {
        enabled = FeatureConfig.getEnabled(configuration, ID, true, COMMENT);
        configuration.get(FeatureConfig.category(ID), "enabled", true, COMMENT)
            .setRequiresMcRestart(true);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        if (Loader.isModLoaded("MineTweaker3") && Loader.isModLoaded("modtweaker2")) {
            // Also expose the name to ordinary scripts, so misplaced definitions get an actionable error.
            MineTweakerAPI.registerClass(CustomAspectsZen.class);
            if (enabled) {
                CustomAspectScriptLoader
                    .load(new File(ThaumicDabblery.configFile.getParentFile(), "thaumicdabblery/aspects").toPath());
            }
            MineTweakerImplementationAPI.onReloadEvent(event1 -> CustomAspectRegistry.verifyRegistrations());
        }
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        CustomAspectRegistry.verifyRegistrations();
    }
}
