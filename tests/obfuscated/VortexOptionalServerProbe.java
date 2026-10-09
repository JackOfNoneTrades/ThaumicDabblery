package tdtest;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.server.MinecraftServer;
@Mod(modid="tdvortexoptionalprobe",name="Vortex optional-dependency probe",version="1",dependencies="required-after:thaumicdabblery")
public final class VortexOptionalServerProbe {
 @Mod.EventHandler public void started(FMLServerStartedEvent e){MinecraftServer server=MinecraftServer.func_71276_C();try{
  if(Loader.isModLoaded("ThaumicHorizons")||Loader.isModLoaded("MineTweaker3")||Loader.isModLoaded("modtweaker2"))throw new AssertionError("Optional mods must be absent");
  EntityItem a=new EntityItem(server.func_71218_a(0),0,100,0,new ItemStack(Items.field_151045_i,1));EntityItem b=new EntityItem(server.func_71218_a(0),0,100,0,new ItemStack(Items.field_151045_i,2));
  if(!a.func_70289_a(b)||!a.field_70128_L||b.func_92059_d().field_77994_a!=3)throw new AssertionError("ordinary item merging changed");
  System.out.println("TD_VORTEX_OPTIONAL_SERVER_PASS");
 }catch(Throwable t){System.out.println("TD_VORTEX_OPTIONAL_SERVER_FAILED");t.printStackTrace();}finally{server.func_71263_m();}}
}
