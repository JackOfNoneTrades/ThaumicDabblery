package tdtest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import minetweaker.MineTweakerImplementationAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.tiles.TileJarFillable;

@Mod(modid = "tdaspectclientprobe", version = "1", dependencies = "required-after:thaumicdabblery", acceptableRemoteVersions = "*")
public final class CustomAspectClientProbe {
    private CustomAspectChecks checks;
    private int ticks;
    private boolean launched;
    private boolean done;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        checks = new CustomAspectChecks();
        checks.registry();
        checks.check("Time, Moment, Eternity".equals(checks.time.getLocalizedDescription()), "fallback description");
        System.out.println("TD_ASPECT_CLIENT_EARLY_PASS");
    }

    @Mod.EventHandler
    public void complete(FMLLoadCompleteEvent event) {
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END || ++ticks < 30) return;
        Minecraft mc = Minecraft.func_71410_x();
        try {
            if (!launched) {
                launched = true;
                String remote = System.getProperty("td.aspects.server");
                if (remote != null) {
                    FMLClientHandler.instance().setupServerList();
                    FMLClientHandler.instance().connectToServer(null, new ServerData("td aspect test", remote));
                } else {
                    mc.func_71371_a("td_custom_aspects", "td custom aspects", new WorldSettings(42L, WorldSettings.GameType.CREATIVE, false, false, WorldType.field_77138_c).func_77166_b());
                }
                ticks = 0;
                return;
            }
            if (mc.field_71439_g == null) {
                checks.check(ticks < 2400, "client joins world within timeout");
                return;
            }
            if (System.getProperty("td.aspects.server") != null) {
                if (Thaumcraft.proxy.playerKnowledge.getAspectPoolFor(mc.field_71439_g.func_70005_c_(), checks.time) != 23
                    || !(mc.field_71441_e.func_147438_o(0, 5, 0) instanceof TileJarFillable)) {
                    checks.check(ticks < 2400, "network sync within timeout");
                    return;
                }
                TileJarFillable jar = (TileJarFillable) mc.field_71441_e.func_147438_o(0, 5, 0);
                if (jar.aspect != checks.time || jar.aspectFilter != checks.time || jar.amount != 17) {
                    checks.check(ticks < 2400, "jar contents synchronize within timeout: " + jar.aspect + ", " + jar.aspectFilter + ", " + jar.amount);
                    return;
                }
                checks.check(jar.aspect == checks.time && jar.aspectFilter == checks.time && jar.amount == 17, "dedicated server jar synchronization");
                System.out.println("TD_ASPECT_NETWORK_RECEIVED");
            }
            done = true;
            checks.scripted();
            checks.validation();
            checks.check("time from script".equals(checks.time.getLocalizedDescription()), "normal language override");
            IResource icon = mc.func_110442_L().func_110536_a(checks.time.getImage());
            checks.check(icon.func_110527_b().read() == 137, "PNG resource loads");
            icon.func_110527_b().close();
            mc.func_110434_K().func_110577_a(checks.time.getImage());
            for (int i = 0; i < 3; i++) {
                MineTweakerImplementationAPI.reload();
                checks.scripted();
                checks.check("time from script".equals(checks.time.getLocalizedDescription()), "language survives reload");
            }
            mc.func_110436_a();
            checks.check("Time, Moment, Eternity".equals(checks.time.getLocalizedDescription()), "fallback survives resource reload");
            checks.check("moment from resource pack".equals(checks.moment.getLocalizedDescription()), "resource translation survives reload");
            MineTweakerImplementationAPI.reload();
            checks.check("time from script".equals(checks.time.getLocalizedDescription()), "script translation reapplies after resource reload");
            System.out.println("TD_ASPECT_CLIENT_ALL_PASS checks=" + checks.checks);
            mc.func_71400_g();
        } catch (Throwable failure) {
            done = true;
            System.out.println("TD_ASPECT_CLIENT_FAILED checks=" + checks.checks);
            failure.printStackTrace();
            mc.func_71400_g();
        }
    }
}
