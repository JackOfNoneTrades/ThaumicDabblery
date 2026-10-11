package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.*;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.*;
import net.minecraft.item.ItemStack;
import com.kentington.thaumichorizons.common.ThaumicHorizons;
import com.kentington.thaumichorizons.common.tiles.*;
import thaumcraft.api.aspects.*;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TileJarFillable;
@Mod(modid="tdrevivalserver",name="Effigy revival server probe",version="1",acceptableRemoteVersions="*",dependencies="required-after:thaumicdabblery;required-after:ThaumicHorizons")
public final class EffigyRevivalServerProbe {
 private TileVat vat;private TileJarFillable life,heal;private int checks;
 private void check(boolean ok,String what){checks++;if(!ok)throw new AssertionError(what);}
 @Mod.EventHandler public void init(FMLInitializationEvent e){FMLCommonHandler.instance().bus().register(this);}
 @Mod.EventHandler public void starting(FMLServerStartingEvent e){e.registerServerCommand(new CommandBase(){
  public String func_71517_b(){return "tdrevival";}public String func_71518_a(ICommandSender s){return "/tdrevival start|feed|verify";}public int func_82362_a(){return 2;}public int compareTo(Object o){return 0;}
  public void func_71515_b(ICommandSender s,String[] a){try{
   if(a[0].equals("start")){vat.sample=new ItemStack(ThaumicHorizons.itemCorpseEffigy);}
   else if(a[0].equals("feed")){check(vat.mode==3,"still unrevived without essentia");life.addToContainer(Aspect.LIFE,8);heal.addToContainer(Aspect.HEAL,8);}
   else {check(vat.mode==4&&vat.sample==null,"native revival completed");check(life.amount==0&&heal.amount==0,"native connectors consumed eight of each aspect");check(vat.selfInfusionHealth==20,"native revived health");System.out.println("TD_REVIVAL_SERVER_PASS checks="+checks);}
  }catch(Throwable t){System.out.println("TD_REVIVAL_SERVER_FAILED");t.printStackTrace();MinecraftServer.func_71276_C().func_71263_m();}}
 });}
 @Mod.EventHandler public void started(FMLServerStartedEvent e){try{
  WorldServer w=MinecraftServer.func_71276_C().func_71218_a(0);w.func_82736_K().func_82764_b("doMobSpawning","false");w.func_82736_K().func_82764_b("doDaylightCycle","false");w.func_72877_b(6000);
  vat=VatFacingChecks.assembled(w,0,7,0);
  for(int x:new int[]{-1,1}){w.func_147465_d(x,3,0,ConfigBlocks.blockJar,0,3);TileJarFillable jar=(TileJarFillable)w.func_147438_o(x,3,0);if(x<0)life=jar;else heal=jar;}
  w.func_147465_d(0,8,0,ThaumicHorizons.blockSoulBeacon,0,3);
  check(w.func_147438_o(-1,4,0) instanceof TileVatConnector,"native connector above life jar");check(w.func_147438_o(1,4,0) instanceof TileVatConnector,"native connector above heal jar");
  System.out.println("TD_REVIVAL_READY");
 }catch(Throwable t){System.out.println("TD_REVIVAL_SERVER_FAILED");t.printStackTrace();MinecraftServer.func_71276_C().func_71263_m();}}
 @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent e){EntityPlayerMP p=(EntityPlayerMP)e.player;MinecraftServer.func_71276_C().func_71203_ab().func_152605_a(p.func_146103_bH());p.func_71033_a(WorldSettings.GameType.CREATIVE);p.field_71075_bZ.field_75100_b=true;p.func_71016_p();p.field_71135_a.func_147364_a(.5,5,-4,0,10);((TileSoulBeacon)p.field_70170_p.func_147438_o(0,8,0)).activate(p);}
}
