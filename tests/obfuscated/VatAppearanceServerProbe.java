package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
@Mod(modid="tdvatappearanceserverprobe",name="Vat appearance server probe",version="1",dependencies="required-after:thaumicdabblery;required-after:ThaumicHorizons;required-after:modtweaker2;after:tc4tweak;after:salisarcana")
public final class VatAppearanceServerProbe {
 @Mod.EventHandler public void started(FMLServerStartedEvent event){VatAppearanceChecks c=new VatAppearanceChecks();try{c.run(FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0));System.out.println("TD_VAT_APPEARANCE_SERVER_PASS checks="+c.checks);}catch(Throwable t){System.out.println("TD_VAT_APPEARANCE_SERVER_FAILED checks="+c.checks);t.printStackTrace();}finally{FMLCommonHandler.instance().getMinecraftServerInstance().func_71263_m();}}
}
