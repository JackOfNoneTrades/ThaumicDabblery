package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.research.ScanManager;
public final class PrimalAspectNetwork {
 private EntityPlayerMP player;private int ticks;
 @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent e){player=(EntityPlayerMP)e.player;ticks=0;Thaumcraft.proxy.getPlayerKnowledge().wipePlayerKnowledge(player.func_70005_c_());}
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END||player==null||++ticks!=180)return;try{
  if(!ScanManager.completeScan(player,new ScanResult((byte)1,Item.func_150891_b(Items.field_151113_aN),0,null,""),"@"))throw new AssertionError("network scan");
  System.out.println("TD_PRIMAL_NETWORK_SCAN");
 }catch(Throwable t){System.out.println("TD_PRIMAL_NETWORK_FAILED");t.printStackTrace();}}
 @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(Boolean.getBoolean("td.primal.network"))FMLCommonHandler.instance().getMinecraftServerInstance().func_71263_m();}
}
