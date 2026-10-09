package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
@Mod(modid="tdplanarserverprobe",name="Planar vortex server probe",version="1",dependencies="required-after:thaumicdabblery;required-after:ThaumicHorizons;required-after:modtweaker2;after:tc4tweak;after:salisarcana")
public final class PlanarVortexServerProbe {
 @Mod.EventHandler public void started(FMLServerStartedEvent event){PlanarVortexChecks c=new PlanarVortexChecks();try{c.run(FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0));PlanarVortexCompletionChecks extra=new PlanarVortexCompletionChecks();extra.run(FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0));c.checks+=extra.checks;PlanarVortexAnimationChecks animation=new PlanarVortexAnimationChecks();animation.run(FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0));c.checks+=animation.checks;System.out.println("TD_PLANAR_SERVER_PASS checks="+c.checks);}catch(Throwable t){System.out.println("TD_PLANAR_SERVER_FAILED checks="+c.checks);t.printStackTrace();}finally{FMLCommonHandler.instance().getMinecraftServerInstance().func_71263_m();}}
}
