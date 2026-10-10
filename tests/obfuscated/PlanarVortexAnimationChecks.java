package tdtest;
import net.minecraft.world.WorldServer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import com.kentington.thaumichorizons.common.tiles.TileVortex;
import org.fentanylsolutions.thaumicdabblery.feature.planarvortex.*;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.config.ConfigItems;
public final class PlanarVortexAnimationChecks {
 public int checks; private WorldServer world; private TileVortex tile;private final java.util.List<String> sounds=new java.util.ArrayList<>();
 private int countSound(String sound){return java.util.Collections.frequency(sounds,sound);}
 private void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
 private void age(int age){world.func_72912_H().func_82572_b(VortexCrafting.state(tile).started+age);tile.func_145845_h();}
 private int diamonds(){int n=0;for(Object e:world.field_72996_f)if(e instanceof EntityItem&&!((EntityItem)e).field_70128_L&&((EntityItem)e).func_92059_d().func_77973_b()==Items.field_151045_i&&tile.getDistanceTo(((EntityItem)e).field_70165_t,((EntityItem)e).field_70163_u,((EntityItem)e).field_70161_v)<12)n+=((EntityItem)e).func_92059_d().field_77994_a;return n;}
 private EntityItem offer(ItemStack stack){EntityItem e=new EntityItem(world,60.5,100.5,40.5,stack);e.func_145799_b("VortexTester");world.func_72838_d(e);((VortexCrafting.Holder)tile).thaumicdabblery$craftInput(e);return e;}
 public void run(WorldServer w)throws Exception{
  world=w;tile=PlanarVortexChecks.vortex(w,60,100,40);
  net.minecraft.world.IWorldAccess observer=(net.minecraft.world.IWorldAccess)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{net.minecraft.world.IWorldAccess.class},(o,m,a)->{
   if(m.getDeclaringClass()==Object.class){if(m.getName().equals("equals"))return o==a[0];if(m.getName().equals("hashCode"))return System.identityHashCode(o);return "Vortex sound observer";}
   if(a!=null&&a.length==6&&a[0] instanceof String)sounds.add((String)a[0]);return null;
  });w.func_72954_a(observer);

  PlanarVortexChecks.script(PlanarVortexChecks.PREFIX+"PlanarVortex.addItemRecipe(\"custom:instant\", <minecraft:dirt>, <minecraft:diamond>);\nPlanarVortex.setCompletion(\"custom:instant\", \"instant\");\nPlanarVortex.addItemRecipe(\"custom:paid\", <minecraft:cookie>, <minecraft:emerald>);\nPlanarVortex.setCompletion(\"custom:paid\", \"wand\", \"aer 5\");\n");
  EntityItem input=offer(new ItemStack(net.minecraft.init.Blocks.field_150346_d));long start=VortexCrafting.state(tile).started;check(start>=0&&!input.field_70128_L&&diamonds()==0,"windup preserves input and defers output");check(countSound(VortexCrafting.OPEN_SOUND)==1&&countSound(VortexCrafting.CLOSE_SOUND)==0,"opening sound starts immediately");
  ((VortexCrafting.Holder)tile).thaumicdabblery$craftInput(input);check(VortexCrafting.state(tile).started==start,"repeated input does not restart animation");check(countSound(VortexCrafting.OPEN_SOUND)==1,"repeated offering does not repeat opening sound");age(21);check(countSound(VortexCrafting.CLOSE_SOUND)==0,"closing sound waits for expansion");age(22);check(countSound(VortexCrafting.CLOSE_SOUND)==1,"closing sound begins exactly at expansion");age(23);check(countSound(VortexCrafting.CLOSE_SOUND)==1,"expansion sound happens once");age(27);check(!input.field_70128_L&&diamonds()==0,"nothing released before beat");age(28);check(input.field_70128_L&&diamonds()==1,"output appears on release beat");age(29);check(diamonds()==1,"release happens once");age(36);check(VortexCrafting.state(tile).started<0,"effect returns to idle");
  EntityPlayerMP player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(w);w.field_73010_i.add(player);ItemStack wand=new ItemStack(ConfigItems.itemWandCasting);NBTTagCompound tags=new NBTTagCompound();tags.func_74778_a("cap","gold");tags.func_74778_a("rod","greatwood");wand.func_77982_d(tags);ItemWandCasting casting=(ItemWandCasting)wand.func_77973_b();casting.storeVis(wand,Aspect.AIR,5000);player.field_71071_by.func_70299_a(0,wand);
  offer(new ItemStack(Items.field_151106_aX));check(VortexRecipes.pending(tile).size()==1&&VortexCrafting.state(tile).started<0,"wand offerings queue without starting release");tile.onWandRightClick(w,wand,player,60,100,40,1,0);check(casting.getVis(wand,Aspect.AIR)==5000&&VortexRecipes.pending(tile).size()==1,"wand windup does not charge vis");start=VortexCrafting.state(tile).started;tile.onWandRightClick(w,wand,player,60,100,40,1,0);check(VortexCrafting.state(tile).started==start,"wand spam cannot restart or enqueue duplicate completions");
  player.field_71071_by.func_70299_a(0,null);age(28);check(casting.getVis(wand,Aspect.AIR)==5000&&VortexRecipes.pending(tile).size()==1,"removing wand cancels without consuming queue or vis");age(36);
  player.field_71071_by.func_70299_a(0,wand);tile.onWandRightClick(w,wand,player,60,100,40,1,0);w.field_73010_i.remove(player);age(28);check(casting.getVis(wand,Aspect.AIR)==5000&&VortexRecipes.pending(tile).size()==1,"disconnect cancels without charge");age(36);w.field_73010_i.add(player);
  tile.onWandRightClick(w,wand,player,60,100,40,1,0);int closes=countSound(VortexCrafting.CLOSE_SOUND);tile.cheat=false;tile.beams=5;age(10);check(VortexCrafting.state(tile).started<0&&casting.getVis(wand,Aspect.AIR)==5000&&VortexRecipes.pending(tile).size()==1,"loss of stabilization cancels safely");check(countSound(VortexCrafting.CLOSE_SOUND)==closes,"cancelled contraction does not play expansion sound");tile.cheat=true;tile.beams=6;
  tile.onWandRightClick(w,wand,player,60,100,40,1,0);tile.onChunkUnload();check(VortexCrafting.state(tile).started<0&&VortexRecipes.pending(tile).size()==1&&casting.getVis(wand,Aspect.AIR)==5000,"chunk unload cancels windup without payment");
  tile.onWandRightClick(w,wand,player,60,100,40,1,0);NBTTagCompound saved=new NBTTagCompound();tile.func_145841_b(saved);TileVortex restored=new TileVortex();restored.func_145839_a(saved);w.func_147455_a(60,100,40,restored);tile=restored;check(VortexCrafting.state(tile).started<0&&VortexRecipes.pending(tile).size()==1&&casting.getVis(wand,Aspect.AIR)==5000,"reload preserves uncharged offering and cancels transient windup");
  tile.onWandRightClick(w,wand,player,60,100,40,1,0);age(27);check(casting.getVis(wand,Aspect.AIR)==5000,"payment still deferred immediately before release");age(28);check(VortexRecipes.pending(tile).isEmpty()&&casting.getVis(wand,Aspect.AIR)==4500,"release pays exactly once");age(29);check(casting.getVis(wand,Aspect.AIR)==4500,"later ticks never repeat payment");age(36);
  check(VortexCrafting.scale(0)==1&&VortexCrafting.scale(12)<.3F&&VortexCrafting.scale(28)>1&&VortexCrafting.scale(36)==1,"contract overshoot and settle");check(VortexCrafting.rays(0)==0&&VortexCrafting.rays(14)>0.9F&&VortexCrafting.rays(36)==0,"rays build and fade completely");
  check(VortexCrafting.rays(20)==0&&VortexCrafting.scale(20)<.3F&&VortexCrafting.rays(22)==0&&VortexCrafting.rays(26)==0,"rays disappear completely before expansion");
  EntityItem removed=offer(new ItemStack(net.minecraft.init.Blocks.field_150346_d));removed.func_70106_y();age(28);check(diamonds()==1,"lost input produces no phantom output");age(36);
  check(VortexCrafting.scale(12)==VortexCrafting.MIN_SCALE&&VortexCrafting.scale(20)==.04F,"full contraction reaches a small point without retiming rays");
  EntityItem pulled=new EntityItem(w,60.5,99.5,39.5,new ItemStack(net.minecraft.init.Blocks.field_150346_d));w.func_72838_d(pulled);pulled.field_70181_x=-.4;((VortexCrafting.Holder)tile).thaumicdabblery$craftInput(pulled);
  age(4);pulled.func_70071_h_();double earlyY=pulled.field_70163_u,earlyDistance=tile.getDistanceTo(pulled.field_70165_t,pulled.field_70163_u,pulled.field_70161_v);check(earlyY>99.5&&!pulled.field_70145_X&&!pulled.field_70122_E,"suction lifts falling input and restores collision state");
  age(8);pulled.func_70071_h_();check(pulled.field_70163_u>earlyY&&tile.getDistanceTo(pulled.field_70165_t,pulled.field_70163_u,pulled.field_70161_v)<earlyDistance,"input approaches center over multiple physics ticks");
  EntityItem peer=new EntityItem(w,60.5,100.5,40.5,new ItemStack(net.minecraft.init.Blocks.field_150346_d));check(!pulled.func_70289_a(peer)&&!peer.func_70289_a(pulled),"active input cannot merge in either direction");
  pulled.field_145804_b=0;pulled.func_70100_b_(player);check(!pulled.field_70128_L&&pulled.func_92059_d().field_77994_a==1,"active input cannot be picked up");
  age(12);pulled.func_70071_h_();check(tile.getDistanceTo(pulled.field_70165_t,pulled.field_70163_u,pulled.field_70161_v)<.00001&&VortexSuction.absorbed(pulled)&&!pulled.field_70128_L,"input reaches center and hides while preserving uncommitted stack");
  TileVortex neighbor=PlanarVortexChecks.vortex(w,61,100,40);((VortexCrafting.Holder)neighbor).thaumicdabblery$craftInput(pulled);check(VortexCrafting.state(neighbor).started<0,"neighbor vortex cannot claim active input");w.func_147468_f(61,100,40);
  tile.onChunkUnload();check(VortexSuction.active(pulled)==null&&!VortexSuction.absorbed(pulled)&&!pulled.field_70128_L,"cancelled suction restores visible unconsumed item");double heldY=pulled.field_70163_u;pulled.func_70071_h_();check(pulled.field_70163_u<heldY,"cancelled input resumes ordinary gravity");pulled.func_70106_y();
  tile.items.add(new ItemStack(Items.field_151055_y));tile.onWandRightClick(w,wand,player,60,100,40,1,0);age(22);age(28);check(tile.items.isEmpty(),"native queued item still completes with replacement sounds");age(36);
  EntityItem golem=offer(new ItemStack(com.kentington.thaumichorizons.common.ThaumicHorizons.itemGolemPowder));age(28);check(golem.field_70128_L,"native golem still completes with replacement sounds");age(36);
  check(countSound("thaumcraft:craftstart")==0&&countSound("thaumcraft:wand")==0,"old crafting and completion sounds removed for custom and native recipes");
  w.func_72848_b(observer);
  w.field_73010_i.remove(player);PlanarVortexChecks.script("");w.func_147468_f(60,100,40);
 }
}
