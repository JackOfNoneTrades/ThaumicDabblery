package tdtest;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.world.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.util.*;
import net.minecraftforge.fluids.*;
import cpw.mods.fml.common.registry.GameRegistry;
import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.blocks.BlockKettle.TileEntityKettle;
import com.emoniph.witchery.crafting.KettleRecipes;
import com.emoniph.witchery.crafting.KettleRecipes.KettleRecipe;
import com.emoniph.witchery.common.*;
import com.emoniph.witchery.util.Coord;
import minetweaker.MineTweakerImplementationAPI;
import org.fentanylsolutions.thaumicdabblery.feature.witcherykettle.*;
import org.fentanylsolutions.thaumicdabblery.feature.witcherykettle.KettleRegistry.Recipe;

public final class WitcheryKettleChecks {
 public int checks; private List<KettleRecipe> original; private byte[] fixture;
 public void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 public static ItemStack stack(String n,int count){return new ItemStack(GameRegistry.findItem("minecraft",n),count);}
 public void snapshot(){original=new ArrayList<>(KettleRecipes.instance().recipes);KettleRegistry.recipes();}
 private Recipe recipe(String id){for(Recipe r:KettleRegistry.recipes())if(r.id.equals(id))return r;return null;}
 private KettleRecipe nativeRecipe(String id){for(KettleRecipe k:KettleRecipes.instance().recipes){Recipe r=KettleRegistry.definition(k);if(r!=null&&r.id.equals(id))return k;}return null;}
 private void script(String s)throws Exception{Files.write(Paths.get("scripts/zz-kettle-checks.zs"),("import mods.witchery.Kettle;\n"+s).getBytes(StandardCharsets.UTF_8));MineTweakerImplementationAPI.reload();}
 private void clearScripts()throws Exception{Files.write(Paths.get("scripts/witchery-kettle.zs"),new byte[0]);script("");}
 private void restore()throws Exception{Files.write(Paths.get("scripts/witchery-kettle.zs"),fixture);script("");}
 private void fixture(){check(KettleRegistry.recipes().size()==40,"39 native minus one plus two custom");check(recipe("custom:diamond").power==100&&recipe("custom:diamond").output.field_77994_a==2,"power and output quantity");check(recipe("custom:cat").power==0,"optional power defaults zero");check(recipe("custom:cat").familiar==1&&recipe("custom:cat").bonus==2&&!recipe("custom:cat").inBook,"collection requirements and book visibility");check(recipe("native:witchery:ingredient/121/1")==null,"one native alternative removed");check(recipe("native:witchery:ingredient/121/0").power==12,"sibling native edited independently");}
 public void registry()throws Exception{
  check(Boolean.FALSE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment")),"obfuscated");fixture=Files.readAllBytes(Paths.get("scripts/witchery-kettle.zs"));fixture();MineTweakerImplementationAPI.reload();fixture();
  script("Kettle.setIngredients(\"custom:diamond\",[<minecraft:dirt>]);Kettle.setPower(\"custom:diamond\",-1);Kettle.setOutput(\"custom:diamond\",<minecraft:diamond>*65);Kettle.setBonus(\"custom:diamond\",32);Kettle.setColor(\"custom:diamond\",-1);Kettle.setFamiliar(\"custom:diamond\",\"pig\");Kettle.removeRecipe(\"missing\");Kettle.setColor(\"custom:cat\",0);");
  fixture();check(recipe("custom:cat").color==0xFF000000,"RGB black becomes opaque and valid after errors");
  for(String in:new String[]{"<minecraft:dirt>*2","<minecraft:dirt>.withTag({bad:1})","<minecraft:wool:*>","<minecraft:glass_bottle>"}){
   script("Kettle.setIngredients(\"custom:diamond\",["+in+",<minecraft:cookie>,<minecraft:apple>,<minecraft:coal>,<minecraft:stick>,<minecraft:wheat>]);");check(recipe("custom:diamond").inputs[0].field_77994_a==1&&!recipe("custom:diamond").inputs[0].func_77942_o()&&recipe("custom:diamond").inputs[0].func_77973_b()==stack("dirt",1).func_77973_b(),"reject unsupported input "+in);
  }
  script("Kettle.clearDimension(\"custom:diamond\");Kettle.setBonus(\"custom:diamond\",1);Kettle.setFamiliar(\"custom:cat\",\"owl\");");check(recipe("custom:diamond").dimension==null&&recipe("custom:cat").familiar==3,"clear dimension and named familiar");check(KettleRegistry.recipes().get(0).id.equals("custom:cat"),"setters retain priority");script("");
  for(Recipe r:KettleRegistry.recipes())if(!r.changed)check(KettleRecipes.instance().recipes.contains(r.original),"unchanged native recipe identity");
  check(nativeRecipe("custom:diamond").getDescription().contains("Dimension: 0"),"native book includes dimension");check(nativeRecipe("custom:cat").getDescription().contains("cat familiar")&&nativeRecipe("custom:cat").getDescription().contains("+2"),"book requirements");
  clearScripts();check(KettleRecipes.instance().recipes.equals(original),"undo restores exact original objects and order");restore();
  Configuration cfg=new Configuration();cfg.get("features.witcherykettle","enabled",true).set(false);new WitcheryKettleFeature().configure(cfg);MineTweakerImplementationAPI.reload();check(KettleRecipes.instance().recipes.equals(original),"disabled feature restores originals");cfg.get("features.witcherykettle","enabled",true).set(true);new WitcheryKettleFeature().configure(cfg);MineTweakerImplementationAPI.reload();fixture();
 }
 private WorldServer world(){return MinecraftServer.func_71276_C().func_71218_a(0);}
 private TileEntityKettle tile(int water){WorldServer w=world();w.func_72964_e(1,1);w.func_147468_f(20,100,20);w.func_147465_d(20,98,20,Blocks.field_150348_b,0,3);w.func_147465_d(20,99,20,Blocks.field_150480_ab,0,3);w.func_147465_d(20,100,20,Witchery.Blocks.KETTLE,0,3);TileEntityKettle t=(TileEntityKettle)w.func_147438_o(20,100,20);t.fill(ForgeDirection.UNKNOWN,new FluidStack(FluidRegistry.WATER,water),true);return t;}
 private void ticks(TileEntityKettle t){for(int i=0;i<20;i++)t.func_145845_h();}
 private boolean toss(String item,int count){EntityItem e=new EntityItem(world(),20.5,100.5,20.5,stack(item,count));Witchery.Blocks.KETTLE.func_149670_a(world(),20,100,20,e);return e.field_70128_L;}
 private void ingredients(String last){for(String s:new String[]{"dirt","cookie","apple","coal","stick",last})check(toss(s,1),"dropped ingredient "+s);}
 private KettleBatch batch(TileEntityKettle t){return ((KettleBatch.Holder)t).td$batch();}
 private FakePlayer player(){FakePlayer p=new FakePlayer(world(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"KettleProbe")){@Override public void func_71120_a(net.minecraft.inventory.Container c){}};p.field_71071_by.func_70299_a(p.field_71071_by.field_70461_c,stack("glass_bottle",1));return p;}
 private void collect(FakePlayer p){Witchery.Blocks.KETTLE.func_149727_a(world(),20,100,20,p,1,0,0,0);}
 public void runtime()throws Exception{
  TileEntityKettle t=tile(500);check(!toss("dirt",1),"partial water rejects dropped ingredient");
  t=tile(1000);Power p=new Power(world());p.amount=99;PowerSources.instance().registerPowerSource(p);
  try{check(toss("dirt",32),"bulk input accepted with native behavior");ticks(t);check(!t.isRuined()&&!t.isReady()&&p.amount==99,"partial valid recipe waits without debit");check(t.func_70301_a(0).field_77994_a==32,"whole dropped stack occupies one ingredient slot");for(String s:new String[]{"cookie","apple","coal","stick","wheat"})check(toss(s,1),"remaining input");ticks(t);check(!t.isReady()&&p.amount==99,"insufficient altar power waits");p.amount=100;ticks(t);check(t.isReady()&&t.func_70301_a(6).field_77994_a==2&&p.amount==0&&p.debits==1,"exact one-time power debit and output count");check(t.func_70301_a(6).func_77978_p().func_74762_e("kettle")==1,"output NBT preserved");ticks(t);check(p.debits==1,"no repeat debit");check(batch(t).familiar==0&&batch(t).bonus==0,"actual producing recipe captured despite same-output cat recipe first");check(!t.func_102008_b(6,t.func_70301_a(6),0),"automation requires bottles");check(toss("glass_bottle",2),"bottles admitted");check(t.func_102008_b(6,t.func_70301_a(6),0),"actual unrestricted recipe permits extraction");ItemStack out=t.func_70298_a(6,2);check(out.field_77994_a==2&&!t.isFilled()&&t.bottleCount()==0&&batch(t)==null,"extract consumes bottles and clears water and batch");}finally{PowerSources.instance().removePowerSource(p);}
  t=tile(1000);ingredients("sugar");ticks(t);check(t.isReady()&&batch(t).familiar==1&&batch(t).bonus==2,"shared-output cat recipe captured");toss("glass_bottle",1);check(!t.func_102008_b(6,t.func_70301_a(6),0),"familiar requirement prevents automation");FakePlayer fp=player();collect(fp);check(fp.field_71071_by.func_70448_g().func_77973_b()==stack("glass_bottle",1).func_77973_b()&&t.isReady(),"missing familiar prevents manual collection");
  script("Kettle.setFamiliar(\"custom:cat\",\"none\");");check(!t.func_102008_b(6,t.func_70301_a(6),0),"already finished batch retains familiar rule after edit");NBTTagCompound familiar=new NBTTagCompound();familiar.func_74774_a("FamiliarSummoned",(byte)1);familiar.func_74768_a("FamiliarType",1);familiar.func_74778_a("FamiliarName","Test cat");com.emoniph.witchery.infusion.Infusion.getNBT(fp).func_74782_a("WITCFamiliar",familiar);collect(fp);check(!t.isReady()&&fp.field_71071_by.func_70448_g().func_77973_b()==stack("diamond",1).func_77973_b(),"active required familiar allows collection");
  t=tile(1000);ingredients("sugar");ticks(t);check(batch(t).familiar==0&&batch(t).bonus==2,"next batch uses edited rule");NBTTagCompound saved=new NBTTagCompound();t.func_145841_b(saved);check(saved.func_74764_b(KettleBatch.TAG),"snapshot serialized");clearScripts();t=tile(1000);t.func_145839_a(saved);check(t.isReady()&&batch(t).familiar==0,"snapshot survives reconstruction with recipe removed");
  fp=player();fp.field_71071_by.field_70460_b[3]=new ItemStack(Witchery.Items.WITCH_HAT);long seed=0;while(new Random(seed).nextDouble()>=.35)seed++;world().field_73012_v.setSeed(seed);collect(fp);check(fp.field_71071_by.func_70448_g().func_77973_b()==stack("diamond",1).func_77973_b()&&fp.field_71071_by.func_70448_g().field_77994_a==3,"saved bonus applies on last item after kettle reset and recipe removal");check(batch(t)==null&&!t.isReady()&&!t.isFilled(),"manual collection clears final batch");
  t=tile(1000);saved.func_82580_o(KettleBatch.TAG);t.func_145839_a(saved);check(batch(t)==null,"old NBT accepted without snapshot");fp=player();collect(fp);check(t.isReady(),"old batch falls back to native output lookup");restore();
  ItemStack[] in=recipe("custom:diamond").inputs;check(KettleRecipes.instance().getResult(in,6,false,world())==nativeRecipe("custom:diamond"),"Overworld restriction matches");check(KettleRecipes.instance().getResult(in,6,false,MinecraftServer.func_71276_C().func_71218_a(-1))==null,"explicit dimension zero is not unrestricted");
  script("Kettle.clearDimension(\"custom:diamond\");Kettle.setPower(\"custom:diamond\",0);Kettle.setColor(\"custom:diamond\",0);");check(KettleRecipes.instance().getResult(in,6,false,MinecraftServer.func_71276_C().func_71218_a(-1))==nativeRecipe("custom:diamond"),"clearing allows Nether");t=tile(1000);toss("wheat",1);ticks(t);check(!t.isRuined()&&t.getLiquidColor()==0xFF000000,"black partial brew remains valid");
  t=tile(1000);toss("bedrock",1);ticks(t);check(t.isRuined(),"invalid ingredient spoils natively");t=tile(1000);toss("dirt",1);world().func_147468_f(20,99,20);ticks(t);check(t.isRuined(),"missing fire spoils natively");
  script("Kettle.setIngredients(\"custom:diamond\",[<minecraft:dirt>,<minecraft:cookie>,<minecraft:apple>,<minecraft:coal>,<minecraft:stick>,<minecraft:dirt>]);Kettle.setPower(\"custom:diamond\",0);");t=tile(1000);ingredients("dirt");ticks(t);check(t.isReady(),"repeated ingredients accepted in distinct slots");
  script("Kettle.addRecipe(\"custom:last\",[<minecraft:dirt>,<minecraft:cookie>,<minecraft:apple>,<minecraft:coal>,<minecraft:stick>,<minecraft:wheat>],<minecraft:emerald>);Kettle.setPower(\"custom:diamond\",1);");t=tile(1000);ingredients("wheat");ticks(t);check(t.isReady()&&t.func_70301_a(6).func_77973_b()==stack("emerald",1).func_77973_b(),"last custom wins, setters do not reorder");
  script("");org.fentanylsolutions.thaumicdabblery.compat.modtweaker.WitcheryKettleZen.dump();
 }
 private static final class Power implements IPowerSource {
  final World world;float amount;int debits;Power(World w){world=w;}
  public World getWorld(){return world;}public Coord getLocation(){return new Coord(20,100,20);}public boolean isLocationEqual(Coord c){return getLocation().equals(c);}
  public boolean consumePower(float n){if(amount<n)return false;amount-=n;debits++;return true;}public float getCurrentPower(){return amount;}public float getRange(){return 16;}public int getEnhancementLevel(){return 0;}public boolean isPowerInvalid(){return false;}
 }
}
