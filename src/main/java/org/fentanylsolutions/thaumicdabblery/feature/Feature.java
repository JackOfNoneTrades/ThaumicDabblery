package org.fentanylsolutions.thaumicdabblery.feature;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/** An independently configurable patch or integration supplied by Thaumic Dabblery. */
public interface Feature {

    String id();

    default void configure(Configuration configuration) {}

    default void preInit(FMLPreInitializationEvent event) {}

    default void init(FMLInitializationEvent event) {}

    default void postInit(FMLPostInitializationEvent event) {}

    default void onConfigReload() {}
}
