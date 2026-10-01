package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
@Mod(modid="tdcustomcreatureserverprobe",name="Creature infusion server probe",version="1",dependencies="required-after:thaumicdabblery;required-after:modtweaker2;required-after:ThaumicHorizons;after:tc4tweak;after:salisarcana")
public final class CustomCreatureServerProbe {
 @Mod.EventHandler public void started(FMLServerStartedEvent event){
  CustomCreatureChecks checks=new CustomCreatureChecks();
  try{checks.run(FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0));System.out.println("TD_CUSTOM_CREATURE_SERVER_PASS checks="+checks.checks);}
  catch(Throwable t){System.out.println("TD_CUSTOM_CREATURE_SERVER_FAILED checks="+checks.checks);t.printStackTrace();}
  finally{FMLCommonHandler.instance().getMinecraftServerInstance().func_71263_m();}
 }
}
