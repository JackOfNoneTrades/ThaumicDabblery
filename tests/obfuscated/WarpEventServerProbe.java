package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.EntityPlayerMP;
@Mod(modid="tdwarpserver",name="Warp event server probe",version="1",dependencies="required-after:thaumicdabblery",acceptableRemoteVersions="*")
public final class WarpEventServerProbe {
 private final WarpEventChecks c=new WarpEventChecks();private boolean done;private int ticks;
 @Mod.EventHandler public void started(FMLServerStartedEvent e){MinecraftServer s=MinecraftServer.func_71276_C();EntityPlayerMP p=null;try{c.registry();if(Boolean.getBoolean("td.warp.network"))FMLCommonHandler.instance().bus().register(this);System.out.println("TD_WARP_SERVER_PASS checks="+c.checks);}catch(Throwable t){System.out.println("TD_WARP_SERVER_FAILED checks="+c.checks);t.printStackTrace();}finally{if(p!=null)s.func_71203_ab().field_72404_b.remove(p);if(!Boolean.getBoolean("td.warp.network"))s.func_71263_m();}}
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END)return;MinecraftServer s=MinecraftServer.func_71276_C();if(done){if(++ticks>130)s.func_71263_m();return;}EntityPlayerMP p=s.func_71203_ab().func_152612_a("Developer");if(p==null||++ticks<80)return;done=true;try{c.runtime(p);System.out.println("TD_WARP_NETWORK_PASS checks="+c.checks);p.func_145747_a(new net.minecraft.util.ChatComponentText("TD_WARP_NETWORK_PASS"));}catch(Throwable t){System.out.println("TD_WARP_NETWORK_FAILED checks="+c.checks);t.printStackTrace();s.func_71263_m();}}
}
