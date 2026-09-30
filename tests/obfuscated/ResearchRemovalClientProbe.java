package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.*;
import thaumcraft.api.research.*;
import thaumcraft.client.gui.GuiResearchBrowser;
import java.lang.reflect.Field;
import java.util.ArrayList;
@Mod(modid="tdremovalclientprobe",name="Research removal client probe",version="1",dependencies="required-after:thaumicdabblery;required-after:modtweaker2")
public final class ResearchRemovalClientProbe {
 private final ResearchRemovalChecks checks=new ResearchRemovalChecks();
 private int ticks,frames,stage;private boolean launched,done;private volatile boolean serverDone;private volatile Throwable failure;
 @Mod.EventHandler public void ready(FMLLoadCompleteEvent e){FMLCommonHandler.instance().bus().register(this);}
 @Mod.EventHandler public void started(FMLServerStartedEvent e){try{checks.run();}catch(Throwable t){failure=t;}finally{serverDone=true;}}
 private void open(Minecraft mc)throws Exception{
  Field selected=GuiResearchBrowser.class.getDeclaredField("selectedCategory");selected.setAccessible(true);selected.set(null,ResearchRemovalChecks.SURVIVES);
  GuiResearchBrowser.completedResearch.put(mc.field_71439_g.func_70005_c_(),new ArrayList<String>());
  mc.func_147108_a(new GuiResearchBrowser());
  mc.field_71462_r.func_73863_a(0,0,0);frames=0;
 }
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END||done||++ticks<30)return;Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;
  try{
   if(!launched){launched=true;ticks=0;mc.func_71371_a("td_removal_"+System.currentTimeMillis(),"Research removal",new WorldSettings(42,WorldSettings.GameType.CREATIVE,false,false,WorldType.field_77138_c).func_77166_b());return;}
   if(failure!=null)throw new AssertionError("integrated checks",failure);
   if(ticks>1500)throw new AssertionError("client timeout");
   if(!serverDone||mc.field_71439_g==null||ticks<100)return;
   if(stage>0&&frames<5)return;
   if(stage==0){checks.script(ResearchRemovalChecks.command("removeTab",ResearchRemovalChecks.REMOVED));checks.verifyDetached("TD_A","TD_B");open(mc);stage++;}
   else if(stage==1){
    checks.check(mc.field_71462_r instanceof GuiResearchBrowser,"surviving tab renders after duplicate links removed");
    net.minecraft.util.ScreenShotHelper.func_148259_a(new java.io.File("."),"research-tab-removed.png",mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());
    checks.script("");checks.verifyRestored();open(mc);stage++;
   }else if(stage==2){
    checks.check(mc.field_71462_r instanceof GuiResearchBrowser,"restored duplicate links render after reload");
    checks.script(ResearchRemovalChecks.command("removeTab",ResearchRemovalChecks.REMOVED)+ResearchRemovalChecks.command("removeTab",ResearchRemovalChecks.SECOND));
    checks.verifyDetached("TD_A","TD_B","TD_C");open(mc);stage++;
   }else{
    checks.check(mc.field_71462_r instanceof GuiResearchBrowser,"surviving tab renders after multiple deletions");
    checks.script("");checks.verifyRestored();System.out.println("TD_REMOVAL_CLIENT_PASS checks="+checks.checks);done=true;mc.func_71400_g();
   }
  }catch(Throwable t){done=true;System.out.println("TD_REMOVAL_CLIENT_FAILED checks="+checks.checks);t.printStackTrace();mc.func_71400_g();}
 }
 @SubscribeEvent public void render(TickEvent.RenderTickEvent e){if(e.phase==TickEvent.Phase.END)frames++;}
}
