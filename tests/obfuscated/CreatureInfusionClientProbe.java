package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.*;
import thaumcraft.api.research.*;
import thaumcraft.client.gui.GuiResearchRecipe;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.CreatureInfusionZen;
import java.util.*;
@Mod(modid="tdcreatureclientprobe",name="Creature infusion client probe",version="1",dependencies="required-after:thaumicdabblery;required-after:modtweaker2;required-after:ThaumicHorizons")
public final class CreatureInfusionClientProbe {
 private final CreatureInfusionChecks checks=new CreatureInfusionChecks();
 private final String[] keys={"upgrade:4","transform:Cow->ThaumicHorizons.ChocolateCow","transform:Sheep->ThaumicHorizons.Sheeder","transform:Spider->ThaumicHorizons.Sheeder","transform:ThaumicHorizons.Endersteed->ThaumicHorizons.NightmareTH"};
 private int ticks,frames,stage;private boolean launched,done;private volatile boolean serverDone;private volatile Throwable failure;
 @Mod.EventHandler public void ready(FMLLoadCompleteEvent e){FMLCommonHandler.instance().bus().register(this);}
 @Mod.EventHandler public void started(FMLServerStartedEvent e){try{checks.run(FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0));}catch(Throwable t){failure=t;}finally{serverDone=true;}}
 private void open(Minecraft mc,String key,boolean removed)throws Exception{
  for(Object slot:(List<?>)CreatureInfusionChecks.field(CreatureInfusionZen.class,"SLOTS"))if(Arrays.asList((Object[])CreatureInfusionChecks.field(slot,"entries")).contains(checks.entries().get(key))){
   ResearchItem r=(ResearchItem)CreatureInfusionChecks.field(slot,"research");ResearchPage page=(ResearchPage)CreatureInfusionChecks.field(slot,"current");int index=removed?0:Arrays.asList(r.getPages()).indexOf(page);
   checks.check(index>=0,"updated page attached before rendering");mc.func_147108_a(new GuiResearchRecipe(r,index,0,0));frames=0;return;
  }
  throw new AssertionError("no GUI page for "+key);
 }
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END||done||++ticks<30)return;Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;
  try{
   if(!launched){launched=true;ticks=0;mc.func_71371_a("td_creature_"+System.currentTimeMillis(),"Creature infusions",new WorldSettings(42,WorldSettings.GameType.CREATIVE,false,false,WorldType.field_77138_c).func_77166_b());return;}
   if(failure!=null)throw new AssertionError("integrated checks",failure);if(ticks>1800)throw new AssertionError("client timeout");
   if(!serverDone||mc.field_71439_g==null||ticks<100||(stage>0&&frames<5))return;
   if(stage==0){checks.check(net.minecraft.server.MinecraftServer.func_71276_C().func_71187_D().func_71556_a(mc.field_71439_g,"mt creatureInfusions")==1,"real player can list creature recipes");}
   if(stage>0){checks.check(mc.field_71462_r instanceof GuiResearchRecipe,"native GUI rendered stage "+stage);net.minecraft.util.ScreenShotHelper.func_148259_a(new java.io.File("."),"creature-page-"+stage+".png",mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());}
   if(stage<keys.length){String key=keys[stage];checks.script(CreatureInfusionChecks.set(key,"",3,"ignis 5","<minecraft:cookie>"));open(mc,key,false);stage++;}
   else if(stage==keys.length){checks.script(CreatureInfusionChecks.remove("upgrade:4"));open(mc,"upgrade:4",true);stage++;}
   else if(stage==keys.length+1){checks.script("");checks.restored();open(mc,"upgrade:4",false);stage++;}
   else{System.out.println("TD_CREATURE_CLIENT_PASS checks="+checks.checks);done=true;mc.func_71400_g();}
  }catch(Throwable t){done=true;System.out.println("TD_CREATURE_CLIENT_FAILED checks="+checks.checks);t.printStackTrace();mc.func_71400_g();}
 }
 @SubscribeEvent public void render(TickEvent.RenderTickEvent e){if(e.phase==TickEvent.Phase.END)frames++;}
}
