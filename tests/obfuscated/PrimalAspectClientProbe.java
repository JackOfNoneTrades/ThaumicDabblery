package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.*;
import thaumcraft.api.aspects.*;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.research.*;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import java.lang.reflect.*;
import java.util.*;

@Mod(modid="tdprimalclientprobe",version="1",dependencies="required-after:thaumicdabblery",acceptableRemoteVersions="*")
public final class PrimalAspectClientProbe {
 private String screenshot;
 private int ticks;private boolean launched,done,prechecked;private volatile Throwable failure;private volatile boolean serverDone;
 private PrimalAspectChecks checks;
 @Mod.EventHandler public void complete(FMLLoadCompleteEvent e){checks=new PrimalAspectChecks();FMLCommonHandler.instance().bus().register(this);FMLCommonHandler.instance().bus().register(new PrimalAspectNetwork());}
 @Mod.EventHandler public void started(FMLServerStartedEvent e){try{checks.run(FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0));}catch(Throwable t){failure=t;}serverDone=true;}
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent event){
  if(done||event.phase!=TickEvent.Phase.END||++ticks<30)return;
  Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;
  try{
   if(!launched){launched=true;ticks=0;String remote=System.getProperty("td.primal.server");
    if(remote!=null){FMLClientHandler.instance().setupServerList();FMLClientHandler.instance().connectToServer(null,new ServerData("Primal test",remote));}
    else mc.func_71371_a("td_primals_"+System.currentTimeMillis(),"td primals",new WorldSettings(42,WorldSettings.GameType.CREATIVE,false,false,WorldType.field_77138_c).func_77166_b());return;}
   if(failure!=null)throw new AssertionError("integrated server",failure);
   if(ticks>1500)throw new AssertionError("client timeout");
   if(mc.field_71439_g==null||mc.field_71441_e==null)return;
   if(System.getProperty("td.primal.server")==null&&!serverDone)return;
   String name=mc.field_71439_g.func_70005_c_();PlayerKnowledge k=Thaumcraft.proxy.getPlayerKnowledge();
   if(!prechecked){checks.check(!k.hasDiscoveredAspect(name,checks.hidden),"client primal unknown before scan");prechecked=true;renderWorkbench(mc);}
   if(!k.hasDiscoveredAspect(name,checks.hidden)||k.getAspectPoolFor(name,checks.hidden)<=0)return;
   checks.check(k.hasDiscoveredAspect(name,checks.visible),"visible primal client knowledge");
   ItemStack wand=new ItemStack(ConfigItems.itemWandCasting);ItemWandCasting w=(ItemWandCasting)wand.func_77973_b();w.addVis(wand,checks.hidden,6,true);
   List<String> lines=new ArrayList<>();w.func_77624_a(wand,mc.field_71439_g,lines,true);checks.check(lines.toString().contains("\u00a7f6"),"discovered primal in wand tooltip");
   renderWorkbench(mc);
   System.out.println("TD_PRIMAL_CLIENT_PASS checks="+checks.checks+" remote="+(System.getProperty("td.primal.server")!=null));done=true;
  }catch(Throwable t){done=true;System.out.println("TD_PRIMAL_CLIENT_FAILED");t.printStackTrace();}
  if(done && screenshot==null)mc.func_71400_g();
 }
 private void renderWorkbench(Minecraft mc)throws Exception{
  ItemStack amulet=new ItemStack(ConfigItems.itemAmuletVis);thaumcraft.common.items.baubles.ItemAmuletVis am=(thaumcraft.common.items.baubles.ItemAmuletVis)amulet.func_77973_b();
  am.addVis(amulet,checks.hidden,8,true);List<String> amuletLines=new ArrayList<>();am.func_77624_a(amulet,mc.field_71439_g,amuletLines,true);
  checks.check(amuletLines.toString().contains(checks.hidden.getName())==Thaumcraft.proxy.getPlayerKnowledge().hasDiscoveredAspect(mc.field_71439_g.func_70005_c_(),checks.hidden),"amulet tooltip respects discovery");
  TileArcaneWorkbench tile=new TileArcaneWorkbench();tile.func_145834_a(mc.field_71441_e);
  tile.func_70299_a(0,new ItemStack(Items.field_151055_y)); // stick for scripted recipe
  tile.func_70299_a(10,new ItemStack(ConfigItems.itemWandCasting));
  GuiArcaneWorkbench gui=new GuiArcaneWorkbench(mc.field_71439_g.field_71071_by,tile);
  mc.func_147108_a(gui);gui.func_73863_a(0,0,0);
  Field pages=GuiArcaneWorkbench.class.getDeclaredField("td$pages");pages.setAccessible(true);checks.check(pages.getInt(gui)==2,"additional vis costs have a second page");
  Method click=GuiArcaneWorkbench.class.getDeclaredMethod("func_73864_a",int.class,int.class,int.class);click.setAccessible(true);
  click.invoke(gui,(gui.field_146294_l-190)/2+50,(gui.field_146295_m-234)/2+5,0);gui.func_73863_a(0,0,0);
  Field primals=GuiArcaneWorkbench.class.getDeclaredField("primals");primals.setAccessible(true);checks.check(((List<?>)primals.get(gui)).contains(checks.hidden),"custom primal cost rendered on extra page");
  screenshot="primal-workbench-"+(Thaumcraft.proxy.getPlayerKnowledge().hasDiscoveredAspect(mc.field_71439_g.func_70005_c_(),checks.hidden)?"known":"unknown")+".png";

 }
 @SubscribeEvent public void render(TickEvent.RenderTickEvent event){
  if(event.phase!=TickEvent.Phase.END||screenshot==null)return;
  Minecraft mc=Minecraft.func_71410_x();
  net.minecraft.util.ScreenShotHelper.func_148259_a(new java.io.File("."),screenshot,mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());
  screenshot=null;mc.func_147108_a(null);if(done)mc.func_71400_g();
 }

}
