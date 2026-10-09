package org.fentanylsolutions.thaumicdabblery;

import net.minecraft.network.INetHandler;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.CreatureInfusionZen;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.FurnacePagesZen;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.ResearchPrerequisitesZen;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.ResearchTabsZen;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.ThaumicHorizonsSelfInfusionZen;
import org.fentanylsolutions.thaumicdabblery.feature.FeatureManager;
import org.fentanylsolutions.thaumicdabblery.feature.effigyskins.EffigySkinNetwork;
import org.fentanylsolutions.thaumicdabblery.feature.researcheditor.ResearchEditor;
import org.fentanylsolutions.thaumicdabblery.feature.scanall.ScanAllCommand;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

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
        if (Loader.isModLoaded("ThaumicHorizons")) {
            EffigySkinNetwork.initialize();
            org.fentanylsolutions.thaumicdabblery.feature.planarvortex.VortexFeedback.initialize();
            org.fentanylsolutions.thaumicdabblery.feature.vatfacing.VatFacingNetwork.initialize();
        }
        if (Loader.isModLoaded("MineTweaker3") && Loader.isModLoaded("modtweaker2")) {
            ResearchTabsZen.register();
            FurnacePagesZen.register();
            ResearchPrerequisitesZen.register();
            ResearchEditor.register();
            if (Loader.isModLoaded("ThaumicHorizons")) {
                ThaumicHorizonsSelfInfusionZen.register();
                CreatureInfusionZen.register();
                org.fentanylsolutions.thaumicdabblery.compat.modtweaker.PlanarVortexZen.register();
                org.fentanylsolutions.thaumicdabblery.compat.modtweaker.VatZen.register();
            }
        }
    }

    public void postInit(FMLPostInitializationEvent event) {
        FeatureManager.postInit(event);
        if (Loader.isModLoaded("MineTweaker3") && Loader.isModLoaded("modtweaker2")
            && Loader.isModLoaded("ThaumicHorizons")) {
            ThaumicHorizonsSelfInfusionZen.initializeDisplayPages();
            CreatureInfusionZen.initialize();
        }
    }

    public void serverStarting(FMLServerStartingEvent event) {
        FeatureManager.serverStarting(event);
        event.registerServerCommand(new ScanAllCommand());
        if (Loader.isModLoaded("MineTweaker3") && Loader.isModLoaded("modtweaker2")
            && Loader.isModLoaded("ThaumicHorizons")) CreatureInfusionZen.registerCommand();
    }

    public void receiveEffigyBinding(EffigySkinNetwork.Binding binding, INetHandler source) {}

    public void receiveVatFacing(org.fentanylsolutions.thaumicdabblery.feature.vatfacing.VatFacingNetwork.Pose pose,
        INetHandler source) {}

    public void receiveVortexPreview(net.minecraft.nbt.NBTTagCompound data, INetHandler source) {}

    public boolean toggleResearchEditor() {
        throw new IllegalArgumentException("Thaumonomicon editing is only available in single-player.");
    }

    public void onConfigReload() {
        Config.synchronizeConfiguration(ThaumicDabblery.configFile);
        FeatureManager.onConfigReload();
    }
}
