package tdtest;

import cpw.mods.fml.common.*;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.world.*;
import thaumcraft.common.lib.research.ResearchManager;
import makeo.gadomancy.common.aura.AuraResearchManager;

@Mod(modid="tdpylonclientprobe",name="Aura Pylon client probe",version="1",dependencies="required-after:thaumicdabblery;after:gadomancy",acceptableRemoteVersions="*")
public final class AuraPylonClientProbe {
    private int ticks;
    private boolean launched,done;
    private volatile boolean serverDone;
    private volatile Throwable failure;
    private final AuraPylonChecks checks=new AuraPylonChecks();
    @Mod.EventHandler public void post(FMLPostInitializationEvent event) { AuraPylonChecks.capture(); }
    @Mod.EventHandler public void complete(FMLLoadCompleteEvent event) { FMLCommonHandler.instance().bus().register(this); }
    @Mod.EventHandler public void started(FMLServerStartedEvent event) {
        try { checks.run(FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0)); }
        catch(Throwable thrown) {failure=thrown;}
        serverDone=true;
    }
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent event) {
        try { AuraPylonNetwork.prepare((EntityPlayerMP)event.player); }
        catch(Throwable thrown) {failure=thrown;}
    }
    @SubscribeEvent public void serverTick(TickEvent.ServerTickEvent event) { if(event.phase==TickEvent.Phase.START) AuraPylonNetwork.keepFueled(); }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(done || event.phase!=TickEvent.Phase.END || ++ticks<30) return;
        Minecraft mc=Minecraft.func_71410_x(); mc.field_71474_y.field_82881_y=false;
        try {
            if(!launched) {
                checks.check(!Boolean.TRUE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment")),"obfuscated client");
                launched=true; ticks=0;
                String remote=System.getProperty("td.pylon.server");
                if(remote!=null) {
                    FMLClientHandler.instance().setupServerList();
                    FMLClientHandler.instance().connectToServer(null,new ServerData("Pylon test",remote));
                } else mc.func_71371_a("td_pylon", "td pylon",new WorldSettings(42,WorldSettings.GameType.CREATIVE,false,false,WorldType.field_77138_c).func_77166_b());
                return;
            }
            if(failure!=null) throw new AssertionError("integrated server",failure);
            if(ticks>1200) throw new AssertionError("pylon and research synchronization timeout");
            if(mc.field_71439_g==null || mc.field_71441_e==null) return;
            if(System.getProperty("td.pylon.server")==null && !serverDone) return;
            if(ticks%200==0) System.out.println("TD_PYLON_CLIENT_WAIT potion="+mc.field_71439_g.func_70644_a(Potion.field_76427_o)+" research="+ResearchManager.isResearchComplete(mc.field_71439_g.func_70005_c_(),"GADOMANCY.AURA.tdpylon")+" y="+mc.field_71439_g.field_70163_u);
            if(!mc.field_71439_g.func_70644_a(Potion.field_76427_o)) return;
            if(!ResearchManager.isResearchComplete(mc.field_71439_g.func_70005_c_(),"GADOMANCY.AURA.tdpylon")) return;
            checks.check(mc.field_71439_g.func_70660_b(Potion.field_76427_o).func_76459_b()>0,"pylon potion reaches client");
            checks.check(AuraResearchManager.getKnowledge(mc.field_71439_g).contains("tdpylon"),"custom aura appears in knowledge");
            checks.check(AuraResearchManager.getLines("tdpylon").contains("Pylon test water breathing"),"custom aura localization");
            System.out.println("TD_PYLON_CLIENT_PASS checks="+checks.checks+" remote="+(System.getProperty("td.pylon.server")!=null)); done=true;
        } catch(Throwable thrown) {done=true;System.out.println("TD_PYLON_CLIENT_FAILED");thrown.printStackTrace();}
        if(done) mc.func_71400_g();
    }
}
