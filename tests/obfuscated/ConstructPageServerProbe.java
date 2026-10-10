package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
@Mod(modid="tdconstructserver",name="Construct page server probe",version="1",dependencies="required-after:thaumicdabblery",acceptableRemoteVersions="*")
public final class ConstructPageServerProbe {
 @Mod.EventHandler public void started(FMLServerStartedEvent e){ConstructPageChecks c=new ConstructPageChecks();try{c.run();System.out.println("TD_CONSTRUCT_SERVER_PASS checks="+c.checks);}catch(Throwable t){System.out.println("TD_CONSTRUCT_SERVER_FAILED checks="+c.checks);t.printStackTrace();}finally{if(!Boolean.getBoolean("td.construct.network"))FMLCommonHandler.instance().getMinecraftServerInstance().func_71263_m();}}
}
