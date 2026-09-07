package tdtest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import minetweaker.MineTweakerImplementationAPI;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.util.FakePlayerFactory;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketSyncAspects;
import thaumcraft.common.tiles.TileJarFillable;

@Mod(modid = "tdaspectserverprobe", version = "1", dependencies = "required-after:thaumicdabblery", acceptableRemoteVersions = "*")
public final class CustomAspectServerProbe {
    private CustomAspectChecks checks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        checks = new CustomAspectChecks();
        checks.registry();
        checks.check("Time, Moment, Eternity".equals(checks.time.getLocalizedDescription()), "fallback definition before normal scripts");
        System.out.println("TD_ASPECT_EARLY_PASS");
        FMLCommonHandler.instance().bus().register(this);
    }

    @Mod.EventHandler
    public void started(FMLServerStartedEvent event) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        try {
            checks.scripted();
            checks.validation();
            checks.storage(server.func_71218_a(0), FakePlayerFactory.getMinecraft(server.func_71218_a(0)));
            for (int i = 0; i < 3; i++) {
                MineTweakerImplementationAPI.reload();
                checks.scripted();
            }
            Path usage = Paths.get("scripts/custom-aspects-usage.zs");
            Path parked = Paths.get("scripts/custom-aspects-usage.zs.parked");
            Path definition = Paths.get("config/thaumicdabblery/aspects/10-time.zs");
            Path parkedDefinition = Paths.get(definition + ".parked");
            Files.move(usage, parked);
            Files.move(definition, parkedDefinition);
            try {
                MineTweakerImplementationAPI.reload();
                checks.check(Aspect.getAspect("tdtempus") == checks.time, "removing definitions then reloading retains identity");
                checks.check(ResearchCategories.getResearch("TD_ASPECT_PROBE") == null, "ordinary research remains undoable");
            } finally {
                Files.move(parked, usage);
                Files.move(parkedDefinition, definition);
            }
            MineTweakerImplementationAPI.reload();
            checks.scripted();
            System.out.println("TD_ASPECT_SERVER_ALL_PASS checks=" + checks.checks);
        } catch (Throwable failure) {
            System.out.println("TD_ASPECT_SERVER_FAILED checks=" + checks.checks);
            failure.printStackTrace();
        } finally {
            if (!Boolean.getBoolean("td.aspects.network")) server.func_71263_m();
        }
    }

    @SubscribeEvent
    public void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Boolean.getBoolean("td.aspects.network")) return;
        String name = event.player.func_70005_c_();
        Thaumcraft.proxy.playerKnowledge.addDiscoveredAspect(name, checks.time);
        Thaumcraft.proxy.playerKnowledge.setAspectPool(name, checks.time, (short) 23);
        PacketHandler.INSTANCE.sendTo(new PacketSyncAspects(event.player), (EntityPlayerMP) event.player);
        event.player.field_70170_p.func_147465_d(0, 5, 0, ConfigBlocks.blockJar, 0, 3);
        TileJarFillable jar = (TileJarFillable) event.player.field_70170_p.func_147438_o(0, 5, 0);
        jar.aspect = null;
        jar.aspectFilter = null;
        jar.amount = 0;
        jar.addToContainer(checks.time, 17);
        jar.aspectFilter = checks.time;
        event.player.field_70170_p.func_147471_g(0, 5, 0);
        ((EntityPlayerMP) event.player).field_71135_a.func_147364_a(0.5, 6, 3.5, 180, 0);
        System.out.println("TD_ASPECT_NETWORK_SENT");
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (Boolean.getBoolean("td.aspects.network")) FMLCommonHandler.instance().getMinecraftServerInstance().func_71263_m();
    }
}
