package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import com.kentington.thaumichorizons.common.tiles.TileVat;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.nbt.*;
import net.minecraft.item.*;
import net.minecraft.world.WorldServer;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TilePedestal;
/** Run after CustomCreatureServerProbe, copying its pending-custom-vat.dat into this instance. No script dependencies. */
@Mod(modid="tdcustomsavedvatprobe",name="Saved custom vat probe",version="1",dependencies="required-after:thaumicdabblery;required-after:ThaumicHorizons;after:tc4tweak;after:salisarcana")
public final class CustomCreatureSavedVatProbe {
 @Mod.EventHandler public void started(FMLServerStartedEvent event){
  try{
   if(!Boolean.FALSE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment")))throw new AssertionError("production runtime required");
   if(Loader.isModLoaded("MineTweaker3")||Loader.isModLoaded("modtweaker2"))throw new AssertionError("expected scripting mods absent");
   WorldServer world=FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0);
   NBTTagCompound saved=CompressedStreamTools.func_74797_a(new java.io.File("pending-custom-vat.dat"));TileVat vat=new TileVat();vat.func_145834_a(world);vat.func_145839_a(saved);
   world.func_147465_d(2,99,0,ConfigBlocks.blockStoneDevice,1,3);TilePedestal pedestal=(TilePedestal)world.func_147438_o(2,99,0);pedestal.func_70299_a(0,new ItemStack((Item)Item.field_150901_e.func_82594_a("minecraft:rotten_flesh")));
   vat.func_145845_h();if(vat.mode!=2)throw new AssertionError("in-progress mode not restored");vat.instability=0;vat.addToContainer(Aspect.UNDEAD,8);
   for(int i=0;i<30&&vat.mode==2;i++)vat.craftCycle();
   if(vat.mode!=0||!(vat.getEntityContained() instanceof EntityPigZombie)||vat.getEntityContained().func_110138_aP()!=20||!"Bacon".equals(((EntityPigZombie)vat.getEntityContained()).func_94057_bL())||pedestal.func_70301_a(0)!=null)throw new AssertionError("saved transformation failed");
   System.out.println("TD_CUSTOM_CREATURE_RESTART_PASS scriptingMods=false");
  }catch(Throwable t){System.out.println("TD_CUSTOM_CREATURE_RESTART_FAILED");t.printStackTrace();}
  finally{FMLCommonHandler.instance().getMinecraftServerInstance().func_71263_m();}
 }
}
