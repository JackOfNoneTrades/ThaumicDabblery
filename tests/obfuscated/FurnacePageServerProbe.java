package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
@Mod(modid="tdfurnaceserverprobe",name="Furnace page server probe",version="1",dependencies="required-after:thaumicdabblery",acceptableRemoteVersions="*")
public final class FurnacePageServerProbe {
 @Mod.EventHandler public void started(FMLServerStartedEvent event) {
  FurnacePageChecks checks=new FurnacePageChecks();
  try { checks.run(); System.out.println("TD_FURNACE_SERVER_PASS checks="+checks.checks); }
  catch(Throwable t) { System.out.println("TD_FURNACE_SERVER_FAILED checks="+checks.checks); t.printStackTrace(); }
  finally { if(!Boolean.getBoolean("td.furnace.network")) FMLCommonHandler.instance().getMinecraftServerInstance().func_71263_m(); }
 }
}
