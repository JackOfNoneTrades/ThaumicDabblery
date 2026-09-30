package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
@Mod(modid="tdscanallserverprobe",name="Scan all server probe",version="1",dependencies="required-after:thaumicdabblery",acceptableRemoteVersions="*")
public final class ScanAllServerProbe {
 @Mod.EventHandler public void setup(FMLPostInitializationEvent event){ScanAllChecks.setup();FMLCommonHandler.instance().bus().register(new ScanAllNetwork());}
 @Mod.EventHandler public void started(FMLServerStartedEvent event){
  ScanAllChecks checks=new ScanAllChecks();try{checks.run(FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0));System.out.println("TD_SCANALL_SERVER_PASS checks="+checks.checks);}catch(Throwable t){System.out.println("TD_SCANALL_SERVER_FAILED checks="+checks.checks);t.printStackTrace();}
  finally{if(!Boolean.getBoolean("td.scanall.network"))FMLCommonHandler.instance().getMinecraftServerInstance().func_71263_m();}
 }
}
