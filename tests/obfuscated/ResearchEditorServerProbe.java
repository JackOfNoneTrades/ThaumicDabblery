package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;
@Mod(modid="tdeditorserverprobe",name="Research editor server probe",version="1",dependencies="required-after:thaumicdabblery;required-after:modtweaker2;after:tc4tweak;after:salisarcana")
public final class ResearchEditorServerProbe {
 @Mod.EventHandler public void started(FMLServerStartedEvent event){
  ResearchEditorChecks checks=new ResearchEditorChecks();
  try{
   try{ThaumicDabblery.proxy.toggleResearchEditor();throw new AssertionError("dedicated server editor accepted");}catch(IllegalArgumentException expected){checks.check(expected.getMessage().contains("single-player"),"dedicated server refusal");}
   checks.run();System.out.println("TD_EDITOR_SERVER_PASS checks="+checks.checks);
  }catch(Throwable t){System.out.println("TD_EDITOR_SERVER_FAILED checks="+checks.checks);t.printStackTrace();}
  finally{FMLCommonHandler.instance().getMinecraftServerInstance().func_71263_m();}
 }
}
