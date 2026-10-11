package tdtest;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.world.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;
import cpw.mods.fml.common.registry.GameRegistry;
import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.brewing.*;
import com.emoniph.witchery.brewing.action.*;
import com.emoniph.witchery.common.*;
import com.emoniph.witchery.util.Coord;
import minetweaker.MineTweakerImplementationAPI;
import org.fentanylsolutions.thaumicdabblery.feature.witcherycauldron.*;
import org.fentanylsolutions.thaumicdabblery.feature.witcherycauldron.CauldronRecipes.Recipe;
import org.fentanylsolutions.thaumicdabblery.mixins.late.witchery.BrewRegistryAccessor;

public final class WitcheryCauldronChecks {
 public int checks;private Hashtable<BrewItemKey,BrewAction> original;private byte[] fixture;
 public void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 public static ItemStack stack(String n,int count){return new ItemStack(GameRegistry.findItem("minecraft",n),count);}
 private Hashtable<BrewItemKey,BrewAction> actions(){return ((BrewRegistryAccessor)WitcheryBrewRegistry.INSTANCE).td$getIngredients();}
 public void snapshot(){original=new Hashtable<>(actions());}
 private Recipe recipe(String id){for(Recipe r:CauldronRecipes.recipes())if(r.id.equals(id))return r;return null;}
 private void script(String s)throws Exception{Files.write(Paths.get("scripts/zz-cauldron-checks.zs"),("import mods.witchery.Cauldron;\n"+s).getBytes(StandardCharsets.UTF_8));MineTweakerImplementationAPI.reload();}
 private void fixture(){check(CauldronRecipes.recipes().size()==27,"26 native minus one plus two custom");check(recipe("custom:diamond").power==100&&recipe("custom:diamond").output.field_77994_a==2,"script power and quantity");check(recipe("custom:neutral").power==0,"optional power defaults to zero");check(recipe("native:minecraft:beef/0/0")==null,"one native removed");check(recipe("native:minecraft:porkchop/0/0").power==100,"native power override");for(Map.Entry<BrewItemKey,BrewAction> e:original.entrySet())check(actions().get(e.getKey())==e.getValue(),"native action identity unchanged");}
 public void registry()throws Exception {
  check(Boolean.FALSE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment")),"obfuscated environment");
  fixture=Files.readAllBytes(Paths.get("scripts/witchery-cauldron.zs"));fixture();MineTweakerImplementationAPI.reload();fixture();
  script("Cauldron.addRecipe(\"bad\",[],<minecraft:cookie>,<minecraft:diamond>);Cauldron.addRecipe(\"custom:diamond\",[],<minecraft:cookie>,<minecraft:diamond>);Cauldron.addRecipe(\"custom:bad\",[],<minecraft:diamond>,<minecraft:diamond>);Cauldron.setIngredients(\"custom:diamond\",[<minecraft:dirt>*2]);Cauldron.setIngredients(\"custom:diamond\",[<minecraft:dirt>.withTag({x:1})]);Cauldron.setIngredients(\"custom:diamond\",[<minecraft:wool:*>]);Cauldron.setIngredients(\"custom:diamond\",[<minecraft:nether_wart>,<minecraft:nether_wart>]);Cauldron.setIngredients(\"custom:diamond\",[<minecraft:porkchop>]);Cauldron.setTrigger(\"custom:diamond\",<minecraft:bedrock>);Cauldron.setPower(\"custom:diamond\",-1);Cauldron.setOutput(\"custom:diamond\",<minecraft:diamond>*65);Cauldron.removeRecipe(\"missing\");Cauldron.setPower(\"custom:neutral\",7);");
  check(recipe("custom:diamond").power==100&&recipe("custom:diamond").ingredients.length==2,"invalid definitions atomic");check(recipe("custom:bad")==null&&recipe("custom:neutral").power==7,"valid line after errors executes");script("");fixture();
  script("Cauldron.setPower(\"native:witchery:chalkritual/0/1\",12);");check(recipe("native:witchery:chalkritual/0/1").basePower()==12,"one chalk recipe cost");check(recipe("native:witchery:chalkritual/0/0").basePower()==3150&&recipe("native:witchery:chalkritual/0/2").basePower()==2250,"sibling costs unchanged");script("");
  script("Cauldron.setTrigger(\"custom:neutral\",<minecraft:stone_hoe>);Cauldron.setIngredients(\"custom:neutral\",[<minecraft:glass_pane>]);");
  check(WitcheryBrewRegistry.INSTANCE.getActionForItemStack(stack("wooden_hoe",1))==null&&WitcheryBrewRegistry.INSTANCE.getActionForItemStack(stack("bedrock",1))==null,"unused owned actions removed");check(WitcheryBrewRegistry.INSTANCE.getActionForItemStack(stack("stone_hoe",1))!=null,"new trigger installed");script("");
  clearScripts();check(!CauldronRecipes.active()&&CauldronRecipes.recipes().size()==26,"empty scripts restore all recipes");check(actions().equals(original),"owned actions fully removed on undo");restore();
  Configuration cfg=new Configuration();cfg.get("features.witcherycauldron","enabled",true).set(false);new WitcheryCauldronFeature().configure(cfg);MineTweakerImplementationAPI.reload();check(!CauldronRecipes.active()&&actions().equals(original),"disabled integration");cfg.get("features.witcherycauldron","enabled",true).set(true);new WitcheryCauldronFeature().configure(cfg);MineTweakerImplementationAPI.reload();fixture();
 }
 private void clearScripts()throws Exception{Files.write(Paths.get("scripts/witchery-cauldron.zs"),new byte[0]);script("");}
 private void restore()throws Exception{Files.write(Paths.get("scripts/witchery-cauldron.zs"),fixture);script("");}
 private WorldServer world(){return MinecraftServer.func_71276_C().func_71218_a(0);}
 private TileEntityCauldron tile(int water){WorldServer w=world();w.func_72964_e(1,1);w.func_147468_f(20,100,20);w.func_147465_d(20,98,20,Blocks.field_150348_b,0,3);w.func_147465_d(20,99,20,Blocks.field_150480_ab,0,3);w.func_147465_d(20,100,20,Witchery.Blocks.CAULDRON,0,3);TileEntityCauldron t=(TileEntityCauldron)w.func_147438_o(20,100,20);t.fill(ForgeDirection.UNKNOWN,new FluidStack(FluidRegistry.WATER,water),true);return t;}
 private void ticks(TileEntityCauldron t,int n){for(int i=0;i<n;i++)t.func_145845_h();}
 private boolean toss(String item,int count){EntityItem e=new EntityItem(world(),20.5,100.5,20.5,stack(item,count));Witchery.Blocks.CAULDRON.func_149670_a(world(),20,100,20,e);return e.field_70128_L;}
 private NBTTagCompound brew(TileEntityCauldron t){return t.drain(ForgeDirection.UNKNOWN,3000,false).tag;}
 private int output(String item){int n=0;for(Object o:world().field_72996_f)if(o instanceof EntityItem){EntityItem e=(EntityItem)o;if(!e.field_70128_L&&e.func_92059_d().func_77973_b()==stack(item,1).func_77973_b())n+=e.func_92059_d().field_77994_a;}return n;}
 private void clearItems(){for(Object o:new ArrayList<>(world().field_72996_f))if(o instanceof EntityItem)world().func_72973_f((EntityItem)o);}
 public void runtime()throws Exception {
  clearItems();TileEntityCauldron t=tile(3000);check(!toss("nether_wart",1),"cold cauldron rejects");ticks(t,100);check(t.isBoiling(),"native boiling after 100 ticks");check(!toss("cookie",1),"premature trigger rejected");check(toss("nether_wart",1)&&toss("dirt",1),"custom ingredients admitted");check(toss("cookie",1),"custom trigger admitted");check(t.getPower()==100,"total power overrides ingredient costs");check(brew(t).func_74764_b(CauldronRecipes.PENDING),"pending craft saved");
  Power p=new Power(world());p.amount=139;PowerSources.instance().registerPowerSource(p);
  try{ticks(t,400);check(output("diamond")==0&&t.getLiquidQuantity()==3000&&p.amount==139,"insufficient power preserves pending brew");p.amount=140;ticks(t,400);check(output("diamond")==2&&t.getLiquidQuantity()==0&&p.amount==0,"one craft consumes scaled power and water");ticks(t,400);check(output("diamond")==2,"no repeated output");for(Object o:world().field_72996_f)if(o instanceof EntityItem&&((EntityItem)o).func_92059_d().func_77973_b()==stack("diamond",1).func_77973_b())check(((EntityItem)o).func_92059_d().func_77978_p().func_74762_e("cauldron")==1,"output NBT");
  }finally{PowerSources.instance().removePowerSource(p);}
  clearItems();t=tile(2000);ticks(t,100);check(toss("bedrock",1)&&!toss("wooden_hoe",1),"partial cauldron accepts ingredient but rejects trigger");check(t.getLiquidQuantity()==2000,"partial water unchanged");
  t=tile(3000);ticks(t,100);check(toss("bedrock",1)&&toss("wooden_hoe",1),"neutral ingredient and new trigger");check(t.getPower()==0,"zero total power");
  NBTTagCompound saved=new NBTTagCompound();t.func_145841_b(saved);clearScripts();check(WitcheryBrewRegistry.INSTANCE.getActionForItemStack(stack("wooden_hoe",1))==null,"trigger removed during pending craft");
  t=tile(3000);t.func_145839_a(saved);ticks(t,400);check(output("emerald")==2&&t.getLiquidQuantity()==0,"serialized craft completes after definitions and actions removed");restore();
  clearItems();t=tile(3000);ticks(t,100);check(!toss("beef",1),"removed native recipe cannot trigger");check(toss("chicken",1),"unrelated native recipe accepted");ticks(t,400);check(output("cooked_chicken")==1,"native fallback still completes");
  clearItems();t=tile(3000);ticks(t,100);check(toss("nether_wart",1)&&toss("dirt",1),"ordinary brewing still accepts ingredients");check(t.getPower()==50&&!brew(t).func_74767_n("RitualTriggered"),"ordinary potion costs and state unchanged");
  // Last custom alternative wins without changing the existing native ingredient behavior.
  script("Cauldron.addRecipe(\"custom:last\",[<minecraft:nether_wart>,<minecraft:dirt>],<minecraft:cookie>,<minecraft:apple>,0);Cauldron.setPower(\"custom:diamond\",99);");
  check(toss("cookie",1)&&t.getPower()==0,"setter does not move earlier recipe ahead");ticks(t,400);check(output("apple")==1,"last custom wins");script("");
  clearItems();
  // One native white circle lowers the configured base-100 craft from 140 to 120.
  for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)if((Math.abs(dx)==2&&Math.abs(dz)<=1)||(Math.abs(dz)==2&&Math.abs(dx)<=1)){
   world().func_147465_d(20+dx,99,20+dz,Blocks.field_150348_b,0,3);world().func_147465_d(20+dx,100,20+dz,Witchery.Blocks.GLYPH_RITUAL,0,3);
  }
  t=tile(3000);check(com.emoniph.witchery.util.CircleUtil.isSmallCircle(world(),20,100,20,Witchery.Blocks.GLYPH_RITUAL),"native circle scanner recognizes ring");ticks(t,100);check(toss("nether_wart",1)&&toss("dirt",1)&&toss("cookie",1),"recipe within white circle");
  p=new Power(world());p.amount=119;PowerSources.instance().registerPowerSource(p);
  try{ticks(t,300);check(output("diamond")==0&&p.amount==119,"discounted threshold still enforced");p.amount=120;ticks(t,400);check(output("diamond")==2&&p.amount==0,"native white-circle discount applied");}finally{PowerSources.instance().removePowerSource(p);}
  for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)if(dx!=0||dz!=0)world().func_147468_f(20+dx,100,20+dz);
  clearItems();script("Cauldron.setPower(\"native:minecraft:porkchop/0/0\",0);");t=tile(3000);ticks(t,100);check(toss("porkchop",8),"native bulk-drop behavior retained");ticks(t,400);check(output("cookie")==2,"edited native output completes once for dropped stack");script("");
  clearItems();script("Cauldron.addRecipe(\"custom:bucket\",[],<minecraft:water_bucket>,<minecraft:apple>);");check(recipe("custom:bucket")!=null,"trigger-only container recipe");t=tile(3000);ticks(t,100);check(toss("water_bucket",1),"container trigger admitted");check(output("bucket")==1,"native container return");ticks(t,400);check(output("apple")==1,"trigger-only recipe completes");script("");
  clearItems();
  org.fentanylsolutions.thaumicdabblery.compat.modtweaker.WitcheryCauldronZen.dump();
 }
 private static final class Power implements IPowerSource {
  final World world;float amount;Power(World w){world=w;}
  public World getWorld(){return world;}public Coord getLocation(){return new Coord(20,100,20);}public boolean isLocationEqual(Coord c){return getLocation().equals(c);}
  public boolean consumePower(float n){if(amount<n)return false;amount-=n;return true;}public float getCurrentPower(){return amount;}public float getRange(){return 16;}public int getEnhancementLevel(){return 0;}public boolean isPowerInvalid(){return false;}
 }
}
