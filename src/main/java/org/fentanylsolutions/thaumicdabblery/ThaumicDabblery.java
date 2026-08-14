package org.fentanylsolutions.thaumicdabblery;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = ThaumicDabblery.MODID,
    version = Tags.VERSION,
    name = ThaumicDabblery.MODNAME,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:Thaumcraft@[4.2.3.5,);after:MineTweaker3;after:modtweaker2;after:contenttweaker;"
        + "after:witchery;after:ThaumicHorizons",
    guiFactory = ThaumicDabblery.MODGROUP + "." + ThaumicDabblery.MODID + ".gui.GuiFactory",
    customProperties = { @Mod.CustomProperty(k = "license", v = "CC BY 4.0"),
        @Mod.CustomProperty(k = "issueTrackerUrl", v = "https://github.com/JackOfNoneTrades/ThaumicDabblery/issues"),
        @Mod.CustomProperty(k = "backgroundFile", v = "assets/thaumicdabblery/background.png") })
public class ThaumicDabblery {

    public static final String MODID = "thaumicdabblery";
    public static final String MODNAME = "Thaumic Dabblery";
    public static final String MODGROUP = "org.fentanylsolutions";
    public static final Logger LOG = LogManager.getLogger(MODID);

    public static File configFile;
    private static boolean DEBUG_MODE;

    @SidedProxy(
        clientSide = MODGROUP + "." + MODID + ".ClientProxy",
        serverSide = MODGROUP + "." + MODID + ".CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configFile = event.getSuggestedConfigurationFile();
        DEBUG_MODE = System.getenv("MCMODDING_DEBUG_MODE") != null;
        LOG.info("MCMODDING_DEBUG_MODE env var: {}", DEBUG_MODE);
        LOG.info("Using config file {}", configFile);
        proxy.preInit(event);
        LOG.info("debugMode config option: {}", Config.debugMode);
        LOG.info("isDebugMode: {}", isDebugMode());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    public static boolean isDebugMode() {
        return DEBUG_MODE || Config.debugMode;
    }

    public static void debug(String message) {
        if (isDebugMode()) {
            LOG.info("DEBUG: {}", message);
        }
    }
}
