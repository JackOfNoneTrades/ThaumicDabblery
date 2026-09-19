package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import thaumcraft.common.lib.events.EventHandlerEntity;

@Mod(modid="tdchampionclientprobe", name="Champion client probe", version="1", dependencies="required-after:thaumicdabblery")
public final class ChampionClientProbe {
    private int ticks;
    private boolean launched, done;
    private volatile boolean serverFinished;
    private volatile Throwable serverFailure;
    private volatile int mobId = -1;
    private final ChampionChecks checks = new ChampionChecks();
    @Mod.EventHandler public void pre(FMLPreInitializationEvent e) { ChampionChecks.register(this); }
    @Mod.EventHandler public void complete(FMLLoadCompleteEvent e) { FMLCommonHandler.instance().bus().register(this); }
    @Mod.EventHandler public void started(FMLServerStartedEvent e) {
        try {
            net.minecraft.world.WorldServer world = FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0);
            checks.run(world);
        } catch (Throwable failure) { serverFailure = failure; }
        serverFinished = true;
    }
    @SubscribeEvent public void login(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent e) {
        try {
            mobId = checks.clientChampion((net.minecraft.world.WorldServer) e.player.field_70170_p, e.player).func_145782_y();
        } catch (Throwable failure) { serverFailure = failure; }
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) {
        if (done || e.phase != TickEvent.Phase.END || ++ticks < 30) return;
        Minecraft mc = Minecraft.func_71410_x();
        mc.field_71474_y.field_82881_y = false;
        try {
            if (!launched) {
                launched = true; ticks = 0;
                mc.func_71371_a("td_champions", "td champions", new WorldSettings(42L, WorldSettings.GameType.CREATIVE, false, false, WorldType.field_77138_c).func_77166_b());
                return;
            }
            if (serverFailure != null) throw new AssertionError("integrated server checks", serverFailure);
            if (ticks >= 1200) throw new AssertionError("world and champion sync before timeout");
            if (!serverFinished || mc.field_71441_e == null || mc.field_71439_g == null) return;
            net.minecraft.entity.Entity entity = mc.field_71441_e.func_73045_a(mobId);
            if (!(entity instanceof EntityZombie)) return;
            EntityZombie mob = (EntityZombie) entity;
            if (ChampionChecks.status(mob) < 0) return;
            checks.check(mob.func_94056_bM(), "champion name synchronized to client");
            checks.check(mob.func_110138_aP() > 20, "champion health synchronized to client");
            double before = ChampionChecks.status(mob);
            new EventHandlerEntity().entitySpawns(new EntityJoinWorldEvent(mob, mc.field_71441_e));
            checks.check(ChampionChecks.status(mob) == before, "client handler does not reroll");
            EntityZombie local = new EntityZombie(mc.field_71441_e);
            new EventHandlerEntity().entitySpawns(new EntityJoinWorldEvent(local, mc.field_71441_e));
            checks.check(ChampionChecks.status(local) == -2, "client cannot independently convert fresh mobs");
            System.out.println("TD_CHAMPION_CLIENT_PASS checks=" + checks.checks);
            done = true;
        } catch (Throwable failure) {
            done = true; System.out.println("TD_CHAMPION_CLIENT_FAILED checks=" + checks.checks); failure.printStackTrace();
        }
        if (done) mc.func_71400_g();
    }
}
