package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
@Mod(modid="tdeffigyserverprobe",name="Effigy skin server probe",version="1",dependencies="required-after:thaumicdabblery;required-after:ThaumicHorizons;after:tc4tweak;after:salisarcana")
public final class EffigySkinServerProbe {
 @Mod.EventHandler public void started(FMLServerStartedEvent e){EffigySkinChecks c=new EffigySkinChecks();try{c.run(FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0));System.out.println("TD_EFFIGY_SERVER_PASS checks="+c.checks);}catch(Throwable t){System.out.println("TD_EFFIGY_SERVER_FAILED checks="+c.checks);t.printStackTrace();}finally{FMLCommonHandler.instance().getMinecraftServerInstance().func_71263_m();}}
}
