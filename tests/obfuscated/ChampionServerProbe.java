package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import net.minecraft.server.MinecraftServer;

@Mod(modid="tdchampionserverprobe", name="Champion server probe", version="1", dependencies="required-after:thaumicdabblery")
public final class ChampionServerProbe {
    @Mod.EventHandler public void pre(FMLPreInitializationEvent e) { ChampionChecks.register(this); }
    @Mod.EventHandler public void started(FMLServerStartedEvent e) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        ChampionChecks checks = new ChampionChecks();
        try {
            checks.run(server.func_71218_a(0));
            System.out.println("TD_CHAMPION_SERVER_PASS checks=" + checks.checks);
        } catch (Throwable failure) {
            System.out.println("TD_CHAMPION_SERVER_FAILED checks=" + checks.checks); failure.printStackTrace();
        } finally { server.func_71263_m(); }
    }
}
