package org.fentanylsolutions.thaumicdabblery.feature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public final class FeatureManager {

    private static final List<Feature> FEATURES = new ArrayList<>();
    private static final Set<String> FEATURE_IDS = new HashSet<>();

    private static boolean bootstrapped;

    private FeatureManager() {}

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        FeatureBootstrap.registerFeatures();
        bootstrapped = true;
    }

    static void register(Feature feature) {
        if (bootstrapped) {
            throw new IllegalStateException("Feature registration has already completed");
        }
        if (!FEATURE_IDS.add(feature.id())) {
            throw new IllegalArgumentException("Duplicate feature id: " + feature.id());
        }
        FEATURES.add(feature);
    }

    public static int size() {
        return FEATURES.size();
    }

    public static void configure(Configuration configuration) {
        for (Feature feature : FEATURES) {
            feature.configure(configuration);
        }
    }

    public static void preInit(FMLPreInitializationEvent event) {
        for (Feature feature : FEATURES) {
            feature.preInit(event);
        }
    }

    public static void init(FMLInitializationEvent event) {
        for (Feature feature : FEATURES) {
            feature.init(event);
        }
    }

    public static void postInit(FMLPostInitializationEvent event) {
        for (Feature feature : FEATURES) {
            feature.postInit(event);
        }
    }

    public static void onConfigReload() {
        for (Feature feature : FEATURES) {
            feature.onConfigReload();
        }
    }
}
