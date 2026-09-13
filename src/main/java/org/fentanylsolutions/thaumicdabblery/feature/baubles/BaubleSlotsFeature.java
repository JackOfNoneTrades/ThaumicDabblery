package org.fentanylsolutions.thaumicdabblery.feature.baubles;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.BaublesZen;
import org.fentanylsolutions.thaumicdabblery.feature.Feature;
import org.fentanylsolutions.thaumicdabblery.feature.FeatureConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import minetweaker.MineTweakerAPI;

public final class BaubleSlotsFeature implements Feature {

    public static final String ID = "baubleSlots";
    private static volatile boolean enabled = true;

    public static boolean isEnabled() {
        return enabled;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void configure(Configuration config) {
        enabled = FeatureConfig
            .getEnabled(config, ID, true, "Allow scripts to customize wearable items and Baubles Expanded slot types.");
    }

    @Override
    public void init(FMLInitializationEvent event) {
        if (!Loader.isModLoaded("Baubles|Expanded")) return;
        FMLCommonHandler.instance()
            .bus()
            .register(new BaubleReconciler());
        if (FMLCommonHandler.instance()
            .getSide() == Side.CLIENT) MinecraftForge.EVENT_BUS.register(new BaubleTooltip());
        if (Loader.isModLoaded("MineTweaker3") && Loader.isModLoaded("modtweaker2"))
            MineTweakerAPI.registerClass(BaublesZen.class);
    }
}
