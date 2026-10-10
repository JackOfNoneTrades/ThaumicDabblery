package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.EntityPlayerMP;
@Mod(modid="tdritesserver",name="Witchery rites server probe",version="1",dependencies="required-after:thaumicdabblery;required-after:witchery",acceptableRemoteVersions="*")
public final class WitcheryRitesServerProbe {
 private final WitcheryRitesChecks c=new WitcheryRitesChecks();private boolean done;private int ticks;
 @Mod.EventHandler public void loaded(FMLLoadCompleteEvent e){c.snapshot();}
 @Mod.EventHandler public void started(FMLServerStartedEvent e){try{c.registry();c.runtime(null);System.out.println("TD_RITES_SERVER_PASS checks="+c.checks);if(Boolean.getBoolean("td.rites.network"))FMLCommonHandler.instance().bus().register(this);}catch(Throwable t){System.out.println("TD_RITES_SERVER_FAILED checks="+c.checks);t.printStackTrace();MinecraftServer.func_71276_C().func_71263_m();}finally{if(!Boolean.getBoolean("td.rites.network"))MinecraftServer.func_71276_C().func_71263_m();}}
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END)return;MinecraftServer s=MinecraftServer.func_71276_C();if(done){if(++ticks>100)s.func_71263_m();return;}EntityPlayerMP p=s.func_71203_ab().func_152612_a("Developer");if(p==null||++ticks<100)return;done=true;try{c.runtime(p);System.out.println("TD_RITES_NETWORK_PASS checks="+c.checks);p.func_145747_a(new net.minecraft.util.ChatComponentText("TD_RITES_NETWORK_PASS"));ticks=0;}catch(Throwable t){System.out.println("TD_RITES_NETWORK_FAILED checks="+c.checks);t.printStackTrace();s.func_71263_m();}}
}
