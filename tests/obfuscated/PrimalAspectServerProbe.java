package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
@Mod(modid="tdprimalserverprobe",version="1",dependencies="required-after:thaumicdabblery",acceptableRemoteVersions="*")
public final class PrimalAspectServerProbe {
 @Mod.EventHandler public void init(FMLInitializationEvent event){if(Boolean.getBoolean("td.primal.network"))FMLCommonHandler.instance().bus().register(new PrimalAspectNetwork());}
 @Mod.EventHandler public void started(FMLServerStartedEvent event){
  PrimalAspectChecks checks=new PrimalAspectChecks();
  try{checks.run(FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0));System.out.println("TD_PRIMAL_SERVER_PASS checks="+checks.checks);}
  catch(Throwable t){System.out.println("TD_PRIMAL_SERVER_FAILED checks="+checks.checks);t.printStackTrace();}
  finally{if(!Boolean.getBoolean("td.primal.network"))FMLCommonHandler.instance().getMinecraftServerInstance().func_71263_m();}
 }
}
