package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.*;
import thaumcraft.client.gui.GuiResearchBrowser;
import org.fentanylsolutions.thaumicdabblery.feature.researcheditor.BookEditorInput;
import org.fentanylsolutions.thaumicdabblery.feature.scanall.ScanAllCommand;
@Mod(modid="tdeditoroptionalclientprobe",name="Editor optional client probe",version="1",dependencies="required-after:thaumicdabblery")
public final class ResearchEditorOptionalClientProbe {
 private int ticks;private boolean launched,opened,done;
 @Mod.EventHandler public void ready(FMLLoadCompleteEvent e){FMLCommonHandler.instance().bus().register(this);}
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END||done||++ticks<30)return;Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;
  try{
   if(!launched){launched=true;ticks=0;mc.func_71371_a("td_editor_optional_"+System.currentTimeMillis(),"Editor optional",new WorldSettings(42,WorldSettings.GameType.CREATIVE,false,false,WorldType.field_77138_c).func_77166_b());return;}
   if(ticks>1600)throw new AssertionError("timeout");if(mc.field_71439_g==null||ticks<100)return;
   if(!opened){
    if(BookEditorInput.handler!=null)throw new AssertionError("editor registered without scripting dependencies");
    try{new ScanAllCommand().func_71515_b(mc.field_71439_g,new String[]{"edit"});throw new AssertionError("command accepted without dependencies");}catch(net.minecraft.command.CommandException expected){}
    mc.func_147108_a(new GuiResearchBrowser());opened=true;ticks=0;return;
   }
   if(mc.field_71462_r.getClass()!=GuiResearchBrowser.class)throw new AssertionError("normal book replaced");
   System.out.println("TD_EDITOR_OPTIONAL_CLIENT_PASS");done=true;mc.func_71400_g();
  }catch(Throwable t){System.out.println("TD_EDITOR_OPTIONAL_CLIENT_FAILED");t.printStackTrace();done=true;mc.func_71400_g();}
 }
}
