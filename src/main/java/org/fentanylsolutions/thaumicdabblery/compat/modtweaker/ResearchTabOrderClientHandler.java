package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public final class ResearchTabOrderClientHandler {

    private static final ResearchTabOrderClientHandler INSTANCE = new ResearchTabOrderClientHandler();

    private static boolean registered;

    private ResearchTabOrderClientHandler() {}

    public static synchronized void register() {
        if (registered) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
        registered = true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.world.isRemote) {
            ResearchTabOrderRegistry.reapply();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onConfigChange(ConfigChangedEvent.OnConfigChangedEvent event) {
        if ("tc4tweak".equals(event.modID)) {
            ResearchTabOrderRegistry.reapply();
        }
    }
}
