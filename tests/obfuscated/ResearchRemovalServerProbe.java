package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
@Mod(modid="tdremovalserverprobe",name="Research removal server probe",version="1",dependencies="required-after:thaumicdabblery;required-after:modtweaker2")
public final class ResearchRemovalServerProbe {
 @Mod.EventHandler public void started(FMLServerStartedEvent event){
  ResearchRemovalChecks checks=new ResearchRemovalChecks();
  try{checks.run();System.out.println("TD_REMOVAL_SERVER_PASS checks="+checks.checks);}
  catch(Throwable t){System.out.println("TD_REMOVAL_SERVER_FAILED checks="+checks.checks);t.printStackTrace();}
  finally{FMLCommonHandler.instance().getMinecraftServerInstance().func_71263_m();}
 }
}
