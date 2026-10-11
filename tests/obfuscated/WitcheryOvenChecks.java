package tdtest;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.*;
import net.minecraft.world.WorldServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.item.*;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.config.Configuration;
import cpw.mods.fml.common.registry.GameRegistry;
import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.blocks.BlockWitchesOven.*;
import minetweaker.MineTweakerImplementationAPI;
import org.fentanylsolutions.thaumicdabblery.feature.witcheryoven.*;
import org.fentanylsolutions.thaumicdabblery.feature.witcheryoven.OvenRecipes.Recipe;

public final class WitcheryOvenChecks {
 public int checks;
 public void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 public static ItemStack stack(String name,int amount,int meta){return new ItemStack(GameRegistry.findItem("minecraft",name),amount,meta);}
 public static ItemStack stack(String name,int amount){return stack(name,amount,0);}
 public static ItemStack tagged(String name,int n){ItemStack s=stack(name,1);NBTTagCompound tag=new NBTTagCompound();tag.func_74768_a("oven",n);s.func_77982_d(tag);return s;}
 private void script(String text)throws Exception{Files.write(Paths.get("scripts/zz-oven-checks.zs"),("import mods.witchery.Oven;\n"+text).getBytes(StandardCharsets.UTF_8));MineTweakerImplementationAPI.reload();}
 private Recipe select(String n){return OvenRecipes.select(stack(n,1));}
 private void fixture(){check(OvenRecipes.rules().size()==6,"six ordered rules");Recipe r=select("dirt");check(r.input.amount==2&&r.output.field_77994_a==3&&r.requireJars&&r.jars==2&&r.chance==1&&!r.funnels,"script arguments");r=select("cobblestone");check(r.chance==.3&&r.jars==1&&r.funnels&&!r.requireJars,"optional defaults");check(OvenRecipes.select(stack("log",1,1)).output==null,"wildcard exclusion");check(OvenRecipes.select(stack("log",1,0)).id.equals("custom:oak"),"later override");check(OvenRecipes.select(stack("log2",1,0)).id.equals("custom:ore"),"ore ingredient");check(select("stone")==null&&OvenRecipes.select(tagged("stone",1))!=null&&OvenRecipes.select(tagged("stone",2))==null,"exact tagged input");}
 public void registry()throws Exception{
  check(Boolean.FALSE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment")),"obfuscated runtime");fixture();MineTweakerImplementationAPI.reload();fixture();
  script("Oven.addRecipe(\"bad\",<minecraft:dirt>,<minecraft:diamond>);Oven.addRecipe(\"custom:dirt\",<minecraft:stone>,<minecraft:diamond>);Oven.addRecipe(\"custom:bad\",<minecraft:stone>*65,<minecraft:diamond>);Oven.setByproduct(\"custom:dirt\",<minecraft:cookie>,-1);Oven.setByproduct(\"custom:dirt\",<minecraft:cookie>,1,2.0);Oven.setRequireJars(\"custom:oak\",true);Oven.removeRecipe(\"missing\");Oven.addRecipe(\"custom:last\",<minecraft:sand>,<minecraft:emerald>);");
  check(OvenRecipes.rules().size()==7&&select("sand")!=null,"invalid definitions atomic; valid following line executes");script("");fixture();
  script("Oven.setByproduct(\"custom:oak\",<minecraft:cookie>);Oven.removeRecipe(\"custom:oak\");");check(OvenRecipes.select(stack("log",1,0)).output==null,"remove recipe exposes previous exclusion");script("Oven.removeInput(<minecraft:dirt>);");check(select("dirt").output==null,"last exclusion wins");script("Oven.setByproduct(\"custom:ore\",<minecraft:cookie>);");check(OvenRecipes.select(stack("log",1,1)).output==null,"setter does not reorder");script("Oven.clearByproduct(\"custom:dirt\");");check(select("dirt").byproduct==null&&!select("dirt").requireJars,"clear byproduct clears required policy");script("");
  Path path=Paths.get("scripts/witchery-oven.zs");byte[] content=Files.readAllBytes(path);Files.write(path,new byte[0]);MineTweakerImplementationAPI.reload();check(OvenRecipes.rules().isEmpty(),"empty script restores all native behavior");Files.write(path,content);MineTweakerImplementationAPI.reload();fixture();
  Configuration cfg=new Configuration();cfg.get("features.witcheryoven","enabled",true).set(false);new WitcheryOvenFeature().configure(cfg);MineTweakerImplementationAPI.reload();check(OvenRecipes.rules().isEmpty()&&select("dirt")==null,"disabled feature");cfg.get("features.witcheryoven","enabled",true).set(true);new WitcheryOvenFeature().configure(cfg);MineTweakerImplementationAPI.reload();fixture();
  check(FurnaceRecipes.func_77602_a().func_151395_a(stack("log",1,1))!=null,"global furnace recipes intact");
 }
 private TileEntityWitchesOven oven() {WorldServer w=MinecraftServer.func_71276_C().func_71218_a(0);w.func_72964_e(1,1);w.func_147468_f(20,100,20);w.func_147465_d(20,100,20,Witchery.Blocks.OVEN_IDLE,2,3);w.func_72921_c(20,100,20,2,3);return (TileEntityWitchesOven)w.func_147438_o(20,100,20);}
 private void put(TileEntityWitchesOven o,int slot,ItemStack s){o.func_70299_a(slot,s);}
 private int count(TileEntityWitchesOven o,int slot){return o.func_70301_a(slot)==null?0:o.func_70301_a(slot).field_77994_a;}
 private void ticks(TileEntityWitchesOven o,int n){for(int i=0;i<n;i++)o.func_145845_h();}
 private boolean ready(TileEntityWitchesOven o)throws Exception{Method m=o.getClass().getDeclaredMethod("canSmelt");m.setAccessible(true);return (Boolean)m.invoke(o);}
 public void runtime(EntityPlayerMP player)throws Exception{
  TileEntityWitchesOven o=oven();put(o,0,stack("dirt",4));put(o,1,stack("coal",2));ticks(o,200);check(count(o,0)==4&&count(o,1)==2&&o.furnaceBurnTime==0,"missing required jars does not start fuel");put(o,4,Witchery.Items.GENERIC.itemEmptyClayJar.createStack(4));ticks(o,179);check(count(o,2)==0&&o.furnaceCookTime==179,"native 180 tick timing");ticks(o,1);check(count(o,0)==2&&count(o,2)==3&&count(o,3)==2&&count(o,4)==2,"exact input/output/jar/byproduct quantities");ticks(o,180);check(count(o,0)==0&&count(o,2)==6&&count(o,3)==4&&count(o,4)==0,"second craft stacks results");
  o=oven();put(o,0,stack("dirt",1));put(o,4,Witchery.Items.GENERIC.itemEmptyClayJar.createStack(2));check(!ready(o),"insufficient ingredient count blocks");put(o,0,stack("dirt",2));put(o,2,stack("diamond",62));check(!ready(o),"main stack overflow blocks");put(o,2,stack("diamond",61));put(o,3,stack("cookie",63));check(!ready(o),"required byproduct overflow blocks");put(o,3,stack("cookie",62));check(ready(o),"exact full stack allowed");o.smeltItem();check(count(o,2)==64&&count(o,3)==64&&count(o,4)==0,"full capacity boundary craft");
  o=oven();put(o,0,stack("log",1));check(!ready(o),"insufficient override does not fall through to charcoal");put(o,0,stack("log",2,1));check(!ready(o),"native blocked by wildcard");put(o,0,stack("porkchop",1));check(ready(o),"unmatched native food recipe survives");o.smeltItem();check(o.func_70301_a(2).func_77973_b()==stack("cooked_porkchop",1).func_77973_b(),"native main output preserved");
  script("Oven.setByproduct(\"custom:optional\",<minecraft:cookie>,1,1.0,false);");o=oven();put(o,0,stack("cobblestone",3));o.smeltItem();check(count(o,2)==1&&count(o,3)==0,"optional missing jars still cooks");put(o,4,Witchery.Items.GENERIC.itemEmptyClayJar.createStack(2));put(o,3,stack("cookie",64));o.smeltItem();check(count(o,2)==2&&count(o,4)==2,"optional full byproduct leaves jars");put(o,3,null);o.smeltItem();check(count(o,3)==1&&count(o,4)==1,"guaranteed byproduct spends one jar");
  script("Oven.setByproduct(\"custom:optional\",<minecraft:cookie>,1,0.0,false);");o=oven();put(o,0,stack("cobblestone",1));put(o,4,Witchery.Items.GENERIC.itemEmptyClayJar.createStack());o.smeltItem();check(count(o,2)==1&&count(o,3)==0&&count(o,4)==1,"zero chance never spends jar");
  script("Oven.setByproduct(\"custom:optional\",<minecraft:cookie>,0,1.0,false);");o=oven();put(o,0,stack("cobblestone",1));o.smeltItem();check(count(o,3)==1,"zero jar cost produces directly");script("");
  o=oven();put(o,0,tagged("stone",1));put(o,2,tagged("emerald",3));check(!ready(o),"output tag mismatch blocks merge");put(o,2,tagged("emerald",2));o.smeltItem();check(count(o,2)==2&&o.func_70301_a(2).func_77978_p().func_74762_e("oven")==2,"tagged output exact merge");
  o=oven();put(o,0,stack("dirt",2));put(o,1,stack("coal",1));put(o,4,Witchery.Items.GENERIC.itemEmptyClayJar.createStack(2));ticks(o,60);check(o.furnaceCookTime==60,"progress established");MineTweakerImplementationAPI.reload();ticks(o,1);check(o.furnaceCookTime==1,"reload resets active custom progress");put(o,0,stack("cobblestone",1));ticks(o,1);check(o.furnaceCookTime==1,"switch custom input resets progress");
  o=oven();put(o,0,stack("cobblestone",1));put(o,4,Witchery.Items.GENERIC.itemEmptyClayJar.createStack());WorldServer w=(WorldServer)o.func_145831_w();long seed=0;while(new Random(seed).nextDouble()<.30||new Random(seed).nextDouble()>=.55)seed++;
  w.field_73012_v.setSeed(seed);for(int i=0;i<20;i++)check(ready(o),"readiness remains true");check(w.field_73012_v.nextDouble()==new Random(seed).nextDouble(),"readiness consumes no RNG");
  w.field_73012_v.setSeed(seed);o.smeltItem();check(count(o,3)==0&&count(o,4)==1,"failed chance keeps jars");
  put(o,0,stack("cobblestone",1));w.func_147465_d(19,100,20,Witchery.Blocks.OVEN_FUMEFUNNEL,2,3);w.func_72921_c(19,100,20,2,3);w.field_73012_v.setSeed(seed);o.smeltItem();check(count(o,3)==1&&count(o,4)==0,"ordinary side funnel adds native 25 percent");w.func_147468_f(19,100,20);
  o=oven();put(o,0,stack("cobblestone",1));put(o,1,stack("coal",1));put(o,4,Witchery.Items.GENERIC.itemEmptyClayJar.createStack());w.func_147465_d(20,101,20,Witchery.Blocks.OVEN_FUMEFUNNEL,2,3);w.func_72921_c(20,101,20,2,3);ticks(o,159);check(count(o,2)==0&&o.furnaceCookTime==159,"top funnel reduces cook time to 160");ticks(o,1);check(count(o,2)==1,"top funnel native speed retained");w.func_147468_f(20,101,20);
  check(o.func_102007_a(0,stack("dirt",2),1),"automation accepts custom input");check(o.func_102008_b(2,stack("diamond",3),2),"automation extracts main output");check(o.func_102008_b(3,stack("cookie",2),2),"automation extracts byproduct");
  if(player!=null){o=oven();ContainerWitchesOven c=new ContainerWitchesOven(player.field_71071_by,o);for(int i=0;i<36;i++)player.field_71071_by.func_70299_a(i,null);player.field_71071_by.func_70299_a(9,stack("dirt",2));c.func_82846_b(player,5);check(count(o,0)==2,"shift click routes custom non-furnace input");put(o,0,null);player.field_71071_by.func_70299_a(9,stack("log",1,1));c.func_82846_b(player,5);check(count(o,0)==0,"shift click excludes removed input");player.field_71071_by.func_70299_a(9,Witchery.Items.GENERIC.itemEmptyClayJar.createStack(3));c.func_82846_b(player,5);check(count(o,4)==3,"shift click jars use jar slot");}
  org.fentanylsolutions.thaumicdabblery.compat.modtweaker.WitcheryOvenZen.dump();
 }
}
