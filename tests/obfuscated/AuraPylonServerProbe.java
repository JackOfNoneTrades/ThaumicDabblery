package tdtest;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.server.MinecraftServer;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.AuraPylonZen;

@Mod(modid="tdpylonserverprobe", name="Aura Pylon server probe", version="1", dependencies="required-after:thaumicdabblery;after:gadomancy", acceptableRemoteVersions="*")
public final class AuraPylonServerProbe {
    @Mod.EventHandler public void init(FMLInitializationEvent event) { FMLCommonHandler.instance().bus().register(this); }
    @cpw.mods.fml.common.eventhandler.SubscribeEvent public void login(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        if(Boolean.getBoolean("td.pylon.network")) AuraPylonNetwork.prepare((net.minecraft.entity.player.EntityPlayerMP)event.player);
    }
    @cpw.mods.fml.common.eventhandler.SubscribeEvent public void logout(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        if(Boolean.getBoolean("td.pylon.network")) FMLCommonHandler.instance().getMinecraftServerInstance().func_71263_m();
    }
    @cpw.mods.fml.common.eventhandler.SubscribeEvent public void tick(cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent event) {
        if(Boolean.getBoolean("td.pylon.network") && event.phase==cpw.mods.fml.common.gameevent.TickEvent.Phase.START) AuraPylonNetwork.keepFueled();
    }
    @Mod.EventHandler public void post(FMLPostInitializationEvent event) {
        if (Loader.isModLoaded("gadomancy")) AuraPylonChecks.capture();
    }
    @Mod.EventHandler public void started(FMLServerStartedEvent event) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        try {
            if (Boolean.TRUE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment"))) throw new AssertionError("Production runtime required");
            if (Loader.isModLoaded("gadomancy")) {
                AuraPylonChecks checks = new AuraPylonChecks();
                checks.run(server.func_71218_a(0));
                System.out.println("TD_PYLON_SERVER_PASS checks=" + checks.checks + " modtweaker=" + Loader.instance().getIndexedModList().get("modtweaker2").getVersion());
            } else {
                try { AuraPylonZen.clear("aqua"); throw new AssertionError("missing Gadomancy accepted"); }
                catch (IllegalStateException expected) { if (!expected.getMessage().contains("requires Gadomancy")) throw expected; }
                System.out.println("TD_PYLON_ABSENT_PASS");
            }
        } catch (Throwable failure) {
            System.out.println("TD_PYLON_SERVER_FAILED"); failure.printStackTrace(); server.func_71263_m();
        } finally { if(!Boolean.getBoolean("td.pylon.network")) server.func_71263_m(); }
    }
}
