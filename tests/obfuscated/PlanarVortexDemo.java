package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.world.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import thaumcraft.common.config.ConfigItems;
@Mod(modid="tdplanardemo",name="Planar vortex demo",version="1",acceptableRemoteVersions="*",dependencies="required-after:thaumicdabblery;required-after:ThaumicHorizons;required-after:modtweaker2")
public final class PlanarVortexDemo {
 public static final String COMPLETION="\nPlanarVortex.addItemRecipe(\"custom:instant_diamond\", <minecraft:dirt>, <minecraft:diamond>);\nPlanarVortex.setCompletion(\"custom:instant_diamond\", \"instant\");\nPlanarVortex.addEntityRecipe(\"custom:paid_pig\", <minecraft:cookie>, \"Pig\");\nPlanarVortex.setCompletion(\"custom:paid_pig\", \"wand\", \"aer 5, terra 2\");\n";
 public static final String PAGES="mods.thaumcraft.Research.clearPages(\"planarRift\");\nPlanarVortex.addPage(\"planarRift\", \"custom:diamond\");\nPlanarVortex.addPage(\"planarRift\", \"custom:paid_pig\");\nPlanarVortex.addPage(\"planarRift\", \"custom:zombie\");\nPlanarVortex.addPage(\"planarRift\", \"custom:instant_diamond\");\nPlanarVortex.addPage(\"planarRift\", \"builtin:wisps\");\nPlanarVortex.addPage(\"planarRift\", \"builtin:void_golem\");\nPlanarVortex.addPage(\"planarRift\", \"builtin:crystal_wand\");\nPlanarVortex.addPage(\"planarRift\", \"builtin:void_putty\");\n";
 @Mod.EventHandler public void init(FMLInitializationEvent event){FMLCommonHandler.instance().bus().register(this);}
 @Mod.EventHandler public void started(FMLServerStartedEvent event){try{
  WorldServer w=MinecraftServer.func_71276_C().func_71218_a(0);PlanarVortexChecks.script(PlanarVortexChecks.DEMO.replace("IsBaby: 0 as byte", "IsBaby: 1 as byte, Attributes: [{Name: \"generic.movementSpeed\", Base: 0.0 as double}]")+COMPLETION+PAGES);
  w.func_82736_K().func_82764_b("doDaylightCycle","false");w.func_82736_K().func_82764_b("doMobSpawning","false");w.func_72877_b(18000);
  for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)w.func_147465_d(x,3,z,(Block)Block.field_149771_c.func_82594_a("stonebrick"),0,3);
  PlanarVortexChecks.vortex(w,0,5,0);
  System.out.println("TD_PLANAR_DEMO_READY");
 }catch(Throwable t){System.out.println("TD_PLANAR_DEMO_FAILED");t.printStackTrace();}}
 @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent event){if(!(event.player instanceof EntityPlayerMP))return;EntityPlayerMP p=(EntityPlayerMP)event.player;
  MinecraftServer.func_71276_C().func_71203_ab().func_152605_a(p.func_146103_bH());p.func_71033_a(WorldSettings.GameType.SURVIVAL);p.field_71075_bZ.field_75102_a=true;p.func_71016_p();p.field_71135_a.func_147364_a(.5,4,-3,0,0);
  ItemStack wand=new ItemStack(ConfigItems.itemWandCasting);NBTTagCompound tag=new NBTTagCompound();tag.func_74778_a("rod","wood");tag.func_74778_a("cap","iron");wand.func_77982_d(tag);p.field_71071_by.func_70299_a(0,wand);p.field_71071_by.field_70461_c=0;ItemStack charged=wand.func_77946_l();charged.func_77978_p().func_74778_a("cap","gold");charged.func_77978_p().func_74778_a("rod","greatwood");((thaumcraft.common.items.wands.ItemWandCasting)charged.func_77973_b()).storeVis(charged,thaumcraft.api.aspects.Aspect.AIR,5000);((thaumcraft.common.items.wands.ItemWandCasting)charged.func_77973_b()).storeVis(charged,thaumcraft.api.aspects.Aspect.EARTH,5000);p.field_71071_by.func_70299_a(1,charged);p.field_71069_bz.func_75142_b();
 }
}
