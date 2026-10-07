package tdtest;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import com.kentington.thaumichorizons.common.ThaumicHorizons;
import com.kentington.thaumichorizons.common.tiles.TileVat;
import com.kentington.thaumichorizons.common.lib.EntityInfusionProperties;
import net.minecraft.nbt.*;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.world.WorldServer;
import net.minecraft.item.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TilePedestal;

/** Separate JVM, with MineTweaker/ModTweaker absent, using pending-breach-vat.dat from the server probe. */
@Mod(modid="tdbreachrestartprobe",name="Saved breach probe",version="1",dependencies="required-after:thaumicdabblery;required-after:ThaumicHorizons;after:tc4tweak;after:salisarcana")
public final class CreatureBreachRestartProbe {
 private int x,y,z,explosions,spawns;private WorldServer world;private boolean watching;
 @SubscribeEvent public void explosion(ExplosionEvent.Start e){if(watching&&e.world==world){explosions++;if(spawns!=0||world.func_147438_o(x,y,z)!=null||!world.func_147437_c(x,y-1,z)||!world.func_147437_c(x,y-2,z))throw new AssertionError("restart breach order");}}
 @SubscribeEvent public void spawn(EntityJoinWorldEvent e){if(watching&&e.world==world&&e.entity instanceof EntityPigZombie){EntityPigZombie mob=(EntityPigZombie)e.entity;if(!"BreachOutput".equals(mob.func_94057_bL()))return;spawns++;if(explosions!=1||!mob.func_70631_g_()||mob.func_110143_aJ()!=20)throw new AssertionError("restart output NBT/health/order");try{EntityInfusionProperties props=(EntityInfusionProperties)mob.getClass().getMethod("getExtendedProperties",String.class).invoke(mob,"CreatureInfusion");if(props.getInfusionCosts().getAmount(Aspect.UNDEAD)!=8)throw new AssertionError("saved cost");}catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}}}
 @Mod.EventHandler public void started(FMLServerStartedEvent e){
  try{
   if(!Boolean.FALSE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment"))||Loader.isModLoaded("MineTweaker3")||Loader.isModLoaded("modtweaker2"))throw new AssertionError("production without scripting required");
   world=FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0);NBTTagCompound saved=CompressedStreamTools.func_74797_a(new java.io.File("pending-breach-vat.dat"));x=saved.func_74762_e("x");y=saved.func_74762_e("y");z=saved.func_74762_e("z");
   for(int dy=0;dy<4;dy++)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
    boolean center=dx==0&&dz==0,cap=dy==0||dy==3;int md=cap?(center?(dy==0?7:6):(dy==3&&(dx==0||dz==0)?4:5)):(center?0:10);
    world.func_147465_d(x+dx,y-dy,z+dz,cap?ThaumicHorizons.blockVatSolid:(center?ThaumicHorizons.blockVatInterior:ThaumicHorizons.blockVat),md,3);
   }
   TileVat vat=(TileVat)world.func_147438_o(x,y,z);vat.func_145839_a(saved);
   world.func_147465_d(x+2,y-1,z,ConfigBlocks.blockStoneDevice,1,3);((TilePedestal)world.func_147438_o(x+2,y-1,z)).func_70299_a(0,new ItemStack((Item)Item.field_150901_e.func_82594_a("minecraft:cookie")));
   vat.func_145845_h();if(vat.mode!=2||vat.getEntityContained()==null)throw new AssertionError("pending vat restored");
   MinecraftForge.EVENT_BUS.register(this);watching=true;vat.instability=0;vat.addToContainer(Aspect.UNDEAD,8);for(int i=0;i<40&&vat.mode==2;i++)vat.craftCycle();watching=false;
   if(explosions!=1||spawns!=1||vat.getEntityContained()!=null)throw new AssertionError("restart did not release exactly one result");
   System.out.println("TD_BREACH_RESTART_PASS scriptingMods=false");
  }catch(Throwable t){System.out.println("TD_BREACH_RESTART_FAILED");t.printStackTrace();}
  finally{MinecraftForge.EVENT_BUS.unregister(this);FMLCommonHandler.instance().getMinecraftServerInstance().func_71263_m();}
 }
}
