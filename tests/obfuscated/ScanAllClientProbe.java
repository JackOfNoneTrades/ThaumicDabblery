package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.*;
import net.minecraft.item.ItemStack;
import thaumcraft.api.aspects.*;
import thaumcraft.api.research.*;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.research.*;
import org.fentanylsolutions.thaumicdabblery.feature.scanall.*;
import java.util.*;
import java.lang.reflect.*;
@Mod(modid="tdscanallclientprobe",name="Scan all client probe",version="1",dependencies="required-after:thaumicdabblery",acceptableRemoteVersions="*")
public final class ScanAllClientProbe {
 private ScanAllChecks checks=new ScanAllChecks();
 private int ticks,frames;private boolean launched,opened,done,reloaded;private Throwable failure;
 @Mod.EventHandler public void setup(FMLPostInitializationEvent e){ScanAllChecks.setup();FMLCommonHandler.instance().bus().register(this);FMLCommonHandler.instance().bus().register(new ScanAllNetwork());}
 @Mod.EventHandler public void started(FMLServerStartedEvent e){try{checks.run(FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0));}catch(Throwable t){failure=t;}}
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END||done||++ticks<30)return;Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;
  try{
   if(!launched){launched=true;ticks=0;String remote=System.getProperty("td.scanall.server");
    if(remote!=null){FMLClientHandler.instance().setupServerList();FMLClientHandler.instance().connectToServer(null,new ServerData("Scan all test",remote));}
    else mc.func_71371_a("td_scanall_"+System.currentTimeMillis(),"Scan all",new WorldSettings(42,WorldSettings.GameType.CREATIVE,false,false,WorldType.field_77138_c).func_77166_b());return;}
   if(failure!=null)throw new AssertionError("integrated checks",failure);
   if(ticks>1800)throw new AssertionError("client timeout");
   if(mc.field_71439_g==null||mc.field_71441_e==null)return;
   String name=mc.field_71439_g.func_70005_c_();if(!ScanAll.itemsComplete(name)||ticks<230)return;
   PlayerKnowledge k=Thaumcraft.proxy.getPlayerKnowledge();
   if(!opened){
    checks.check(k.getAspectsDiscovered(name).size()==Aspect.aspects.size(),"all aspects synchronized");
    checks.check(k.getAspectPoolFor(name,Aspect.AIR)==37,"existing points synchronized unchanged");
    checks.check(k.getAspectPoolFor(name,Aspect.getAspect("tdhidden"))==0,"hidden primal discovered with zero points");
    checks.check(k.objectsScanned.get(name) instanceof AllScanHistory && k.entitiesScanned.get(name) instanceof AllScanHistory,"both wildcard histories synchronized");
    for(String key:new String[]{"TD_SCAN_ITEM","TD_SCAN_ENTITY","TD_SCAN_ASPECT","TD_SCAN_GATE"})checks.check(ResearchManager.isResearchComplete(name,"@"+key),"clue synchronized "+key);
    checks.check(ScanManager.hasBeenScanned(mc.field_71439_g,ScanAllChecks.item(ScanAllChecks.stack("wool",14))),"client variant scanned");
    long start=System.nanoTime();GuiResearchRecipe gui=new GuiResearchRecipe(ResearchCategories.getResearch("ASPECTS"),2,0,0);System.out.println("TD_SCANALL_GUI_MS="+(System.nanoTime()-start)/1000000L);
    mc.func_147108_a(gui);opened=true;
   }
   ArrayList<ItemStack> sources=ScanAllSources.sources(name).get(Aspect.getAspect("tdhidden"));
   if(sources==null)return;
   boolean clock=false;for(ItemStack s:sources)if(s.func_77973_b()==ScanAllChecks.stack("clock",0).func_77973_b()){clock=true;checks.check(s.field_77994_a==3,"source aspect amount");}
   if(!clock)return;
   if(!reloaded && Loader.isModLoaded("MineTweaker3")){
    reloaded=true;ReloadProbe.reload();
    checks.check(ScanAllSources.sources(name).isEmpty(),"script reload invalidates the source catalog");return;
   }
   Field field=GuiResearchRecipe.class.getDeclaredField("aspectItems");field.setAccessible(true);
   checks.check(field.get(mc.field_71462_r)==ScanAllSources.sources(name),"open native GUI uses live batched catalog");
   if(frames<5)return;
   net.minecraft.util.ScreenShotHelper.func_148259_a(new java.io.File("."),"scan-all-aspects.png",mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());
   System.out.println("TD_SCANALL_CLIENT_PASS checks="+checks.checks+" remote="+(System.getProperty("td.scanall.server")!=null)+" catalog="+GuiResearchRecipe.cache.size());done=true;mc.func_71400_g();
  }catch(Throwable t){done=true;System.out.println("TD_SCANALL_CLIENT_FAILED checks="+checks.checks);t.printStackTrace();mc.func_71400_g();}
 }
 private static final class ReloadProbe {static void reload(){minetweaker.MineTweakerImplementationAPI.reload();}}
 @SubscribeEvent public void render(TickEvent.RenderTickEvent e){if(e.phase==TickEvent.Phase.END&&opened)frames++;}
}
