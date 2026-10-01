package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
@Mod(modid="tdprerequisiteprobe",name="Research prerequisite probe",version="1",dependencies="required-after:thaumicdabblery;required-after:modtweaker2")
public final class ResearchPrerequisiteServerProbe {
 @Mod.EventHandler public void started(FMLServerStartedEvent event){
  ResearchPrerequisiteChecks checks=new ResearchPrerequisiteChecks();
  try{checks.run();System.out.println("TD_PREREQUISITE_PASS checks="+checks.checks);}
  catch(Throwable t){System.out.println("TD_PREREQUISITE_FAILED checks="+checks.checks);t.printStackTrace();}
  finally{FMLCommonHandler.instance().getMinecraftServerInstance().func_71263_m();}
 }
}
