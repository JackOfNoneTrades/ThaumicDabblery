package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
@Mod(modid="tdcreatureserverprobe",name="Creature infusion server probe",version="1",dependencies="required-after:thaumicdabblery;required-after:modtweaker2;required-after:ThaumicHorizons")
public final class CreatureInfusionServerProbe {
 @Mod.EventHandler public void started(FMLServerStartedEvent event){
  CreatureInfusionChecks checks=new CreatureInfusionChecks();
  try{checks.run(FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0));System.out.println("TD_CREATURE_SERVER_PASS checks="+checks.checks);}
  catch(Throwable t){System.out.println("TD_CREATURE_SERVER_FAILED checks="+checks.checks);t.printStackTrace();}
  finally{FMLCommonHandler.instance().getMinecraftServerInstance().func_71263_m();}
 }
}
