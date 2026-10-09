package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
@Mod(modid="tdosmoticserverprobe",name="Osmotic server probe",version="1",dependencies="required-after:thaumicdabblery;required-after:ThaumicTinkerer;required-after:modtweaker2")
public final class OsmoticServerProbe {
 @Mod.EventHandler public void init(FMLPostInitializationEvent e){OsmoticChecks.register();}
 @Mod.EventHandler public void started(FMLServerStartedEvent event){OsmoticChecks c=new OsmoticChecks();try{c.run(FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0));System.out.println("TD_OSMOTIC_SERVER_PASS checks="+c.checks);}catch(Throwable t){System.out.println("TD_OSMOTIC_SERVER_FAILED checks="+c.checks);t.printStackTrace();}finally{FMLCommonHandler.instance().getMinecraftServerInstance().func_71263_m();}}
}
