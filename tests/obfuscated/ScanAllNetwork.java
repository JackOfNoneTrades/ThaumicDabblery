package tdtest;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import thaumcraft.api.aspects.*;
import thaumcraft.common.Thaumcraft;
import org.fentanylsolutions.thaumicdabblery.feature.scanall.ScanAll;
public final class ScanAllNetwork {
 private EntityPlayerMP player;private int ticks;
 @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent e){player=(EntityPlayerMP)e.player;ticks=0;}
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END||player==null||++ticks!=180)return;try{
  if(ScanAll.itemsComplete(player.func_70005_c_())) {System.out.println("TD_SCANALL_NETWORK_REJOIN");return;}
  Thaumcraft.proxy.getPlayerKnowledge().setAspectPool(player.func_70005_c_(),Aspect.AIR,(short)37);
  int result=MinecraftServer.func_71276_C().func_71187_D().func_71556_a(MinecraftServer.func_71276_C(),"td scanall "+player.func_70005_c_());
  if(result!=1)throw new AssertionError("console command target");
  System.out.println("TD_SCANALL_NETWORK_COMMAND");
 }catch(Throwable t){System.out.println("TD_SCANALL_NETWORK_FAILED");t.printStackTrace();}}
}
