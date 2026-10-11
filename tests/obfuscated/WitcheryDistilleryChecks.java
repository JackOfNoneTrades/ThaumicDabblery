package tdtest;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.world.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.util.FakePlayer;
import cpw.mods.fml.common.registry.GameRegistry;
import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.blocks.BlockDistillery.TileEntityDistillery;
import com.emoniph.witchery.blocks.BlockDistillery.ContainerDistillery;
import com.emoniph.witchery.crafting.DistilleryRecipes;
import com.emoniph.witchery.crafting.DistilleryRecipes.DistilleryRecipe;
import com.emoniph.witchery.common.*;
import com.emoniph.witchery.util.Coord;
import minetweaker.MineTweakerImplementationAPI;
import org.fentanylsolutions.thaumicdabblery.feature.witcherydistillery.*;
import org.fentanylsolutions.thaumicdabblery.feature.witcherydistillery.DistilleryRegistry.Recipe;
public final class WitcheryDistilleryChecks {
 public int checks;private List<DistilleryRecipe> original;private String first;private byte[] fixture;
 public void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 public static ItemStack stack(String n,int count){return new ItemStack(GameRegistry.findItem("minecraft",n),count);}
 private ItemStack jars(int n){return Witchery.Items.GENERIC.itemEmptyClayJar.createStack(n);}
 public void snapshot(){original=new ArrayList<>(DistilleryRecipes.instance().recipes);first=DistilleryRegistry.recipes().get(0).id;}
 private Recipe recipe(String id){for(Recipe r:DistilleryRegistry.recipes())if(r.id.equals(id))return r;return null;}
 private void script(String s)throws Exception{Files.write(Paths.get("scripts/zz-distillery-checks.zs"),("import mods.witchery.Distillery;\n"+s).getBytes(StandardCharsets.UTF_8));MineTweakerImplementationAPI.reload();}
 private void clear()throws Exception{Files.write(Paths.get("scripts/witchery-distillery.zs"),new byte[0]);script("");}
 private void restore()throws Exception{Files.write(Paths.get("scripts/witchery-distillery.zs"),fixture);script("");}
 private void fixture(){check(DistilleryRegistry.recipes().size()==12,"nine native plus three custom");check(recipe("custom:diamond").jars==2&&recipe("custom:diamond").outputs[0].field_77994_a==2,"jar cost and output count");check(recipe("custom:solo").jars==0&&recipe("custom:solo").inputs[1]==null,"optional jars and one input");for(DistilleryRecipe k:original)check(DistilleryRecipes.instance().recipes.contains(k),"original identity retained");}
 public void registry()throws Exception{
  check(Boolean.FALSE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment")),"obfuscated");fixture=Files.readAllBytes(Paths.get("scripts/witchery-distillery.zs"));fixture();MineTweakerImplementationAPI.reload();fixture();
  script("Distillery.setJars(\""+first+"\",4);Distillery.setOutputs(\""+first+"\",[<minecraft:apple>*2]);");check(recipe(first).jars==4&&recipe(first).outputs[0].field_77994_a==2,"edit native by stable ID");script("Distillery.removeRecipe(\""+first+"\");");check(recipe(first)==null&&DistilleryRegistry.recipes().size()==11,"remove only one native recipe");script("");fixture();
  script("Distillery.addRecipe(\"custom:diamond\",[<minecraft:stick>],[<minecraft:apple>]);Distillery.addRecipe(\"bad\",[<minecraft:stick>],[<minecraft:apple>]);Distillery.setInputs(\"custom:diamond\",[]);Distillery.setInputs(\"custom:diamond\",[<minecraft:dirt>,<minecraft:cookie>,<minecraft:apple>]);Distillery.setOutputs(\"custom:diamond\",[]);Distillery.setOutputs(\"custom:diamond\",[<minecraft:apple>*65]);Distillery.setJars(\"custom:diamond\",-1);Distillery.setJars(\"custom:diamond\",65);Distillery.removeRecipe(\"missing\");Distillery.setJars(\"custom:solo\",3);");check(recipe("custom:diamond").jars==2&&recipe("custom:diamond").outputs[0].field_77994_a==2,"invalid changes atomic");check(recipe("custom:solo").jars==3,"valid line after errors runs");
  for(String in:new String[]{"<minecraft:dirt>*2","<minecraft:dirt>.withTag({bad:1})","<minecraft:wool:*>"}){script("Distillery.setInputs(\"custom:diamond\",["+in+"]); ");check(recipe("custom:diamond").inputs[1]!=null,"unsupported input rejected "+in);}
  script("Distillery.setInputs(\"custom:solo\",[<minecraft:apple>]);Distillery.setOutputs(\"custom:solo\",[<minecraft:diamond>]);");check(recipe("custom:solo").inputs[0].func_77973_b()==stack("apple",1).func_77973_b()&&recipe("custom:solo").outputs[0].func_77973_b()==stack("diamond",1).func_77973_b(),"input and output setters");script("");
  clear();check(DistilleryRecipes.instance().recipes.equals(original),"undo restores exact native identities/order");restore();
  Configuration cfg=new Configuration();cfg.get("features.witcherydistillery","enabled",true).set(false);new WitcheryDistilleryFeature().configure(cfg);MineTweakerImplementationAPI.reload();check(DistilleryRecipes.instance().recipes.equals(original),"disabled scripting restores originals");cfg.get("features.witcherydistillery","enabled",true).set(true);new WitcheryDistilleryFeature().configure(cfg);MineTweakerImplementationAPI.reload();fixture();
  for(DistilleryRecipe k:original)check(DistilleryRecipes.instance().getDistillingResult(k.inputs[0],k.inputs[1],jars(64))==k,"native matching remains valid");
 }
 private WorldServer world(){return MinecraftServer.func_71276_C().func_71218_a(0);}
 private TileEntityDistillery tile(){WorldServer w=world();w.func_72964_e(1,1);w.func_147468_f(20,100,20);w.func_147465_d(20,100,20,Witchery.Blocks.DISTILLERY_IDLE,0,3);return (TileEntityDistillery)w.func_147438_o(20,100,20);}
 private void ticks(TileEntityDistillery t,int n){for(int i=0;i<n;i++)t.func_145845_h();}
 private void inputs(TileEntityDistillery t,String a,String b,int jarCount){t.func_70299_a(0,a==null?null:stack(a,3));t.func_70299_a(1,b==null?null:stack(b,3));t.func_70299_a(2,jarCount==0?null:jars(jarCount));}
 private static final class Power implements IPowerSource{
  final World w;float amount;int debits;Power(World w){this.w=w;}public World getWorld(){return w;}public Coord getLocation(){return new Coord(20,100,20);}public boolean isLocationEqual(Coord c){return getLocation().equals(c);}public boolean consumePower(float n){if(amount<n)return false;amount-=n;debits++;return true;}public float getCurrentPower(){return amount;}public float getRange(){return 16;}public int getEnhancementLevel(){return 0;}public boolean isPowerInvalid(){return false;}
 }
 public void runtime()throws Exception{
  Power p=new Power(world());p.amount=10000;PowerSources.instance().registerPowerSource(p);
  try{
   TileEntityDistillery t=tile();inputs(t,"cookie","dirt",1);ticks(t,20);check(t.furnaceCookTime==0&&p.debits==0,"insufficient jars no progress or power use");t.func_70299_a(2,stack("apple",64));ticks(t,20);check(t.getActiveRecipe()==null&&p.debits==0,"wrong item cannot pay jar cost");t.func_70299_a(2,jars(5));
   t.func_70299_a(3,stack("cookie",1));ticks(t,20);check(p.debits==0&&t.func_70301_a(0).field_77994_a==3,"incompatible output blocks without consumption");t.func_70299_a(3,stack("diamond",1));ticks(t,20);check(p.debits==0,"different output NBT blocks");ItemStack out=recipe("custom:diamond").outputs[0].func_77946_l();out.field_77994_a=63;t.func_70299_a(3,out);ticks(t,20);check(p.debits==0,"full output capacity blocks");out.field_77994_a=62;t.func_70299_a(3,out);
   ticks(t,799);check(t.furnaceCookTime==799&&p.debits==799&&t.func_70301_a(0).field_77994_a==3,"native 800-tick duration and deferred consumption");ticks(t,1);check(t.furnaceCookTime==0&&p.debits==800&&t.func_70301_a(3).field_77994_a==64,"native completion after 800 paid ticks");check(t.func_70301_a(4).field_77994_a==1&&t.func_70301_a(5).field_77994_a==3&&t.func_70301_a(6).field_77994_a==4,"all four outputs");check(t.func_70301_a(0).field_77994_a==2&&t.func_70301_a(1).field_77994_a==2&&t.func_70301_a(2).field_77994_a==3,"one each input and exact jars consumed with swapped inputs");check(Math.abs(p.amount-9520)<1,"native 480 total power within float rounding");ticks(t,20);check(p.debits==800,"full output prevents another debit");
   t=tile();inputs(t,null,"stick",0);p.amount=0;ticks(t,20);check(t.furnaceCookTime==0,"zero-jar recipe still needs native altar power");p.amount=10000;ticks(t,100);check(t.furnaceCookTime==100,"one-input recipe works in second slot");p.amount=0;ticks(t,30);check(t.furnaceCookTime==100,"power outage pauses progress");p.amount=10000;
   NBTTagCompound saved=new NBTTagCompound();t.func_145841_b(saved);t=tile();t.func_145839_a(saved);ticks(t,1);check(t.furnaceCookTime==101,"saved progress survives same recipe");script("");ticks(t,1);check(t.furnaceCookTime==102,"identical script reload retains progress");script("Distillery.setOutputs(\"custom:solo\",[<minecraft:apple>]);");ticks(t,1);check(t.furnaceCookTime==1,"changed recipe resets old progress");ticks(t,799);check(t.func_70301_a(3).func_77973_b()==stack("apple",1).func_77973_b(),"edited result after complete new cycle");
   t=tile();inputs(t,"stick",null,0);ticks(t,100);script("Distillery.removeRecipe(\"custom:solo\");");int debits=p.debits;ticks(t,10);check(t.furnaceCookTime==0&&p.debits==debits&&t.func_70301_a(0).field_77994_a==3,"removed recipe cancels progress without consuming inputs");script("");
   t=tile();inputs(t,"cookie",null,1);ticks(t,10);check(t.getActiveRecipe()==null,"one stack cannot satisfy both duplicate inputs");t.func_70299_a(1,stack("cookie",1));ticks(t,800);check(t.func_70301_a(3).func_77973_b()==stack("coal",1).func_77973_b()&&t.func_70301_a(2)==null,"duplicate inputs in separate slots and jar consumed");
   script("Distillery.addRecipe(\"custom:priority\",[<minecraft:stick>],[<minecraft:diamond>],2);Distillery.setJars(\"custom:solo\",0);");t=tile();inputs(t,"stick",null,0);check(t.getActiveRecipe()==null,"missing jars cannot fall back to cheaper matching recipe");t.func_70299_a(2,jars(2));ticks(t,800);check(t.func_70301_a(3).func_77973_b()==stack("diamond",1).func_77973_b(),"last custom wins, setter keeps order");script("");
   t=tile();FakePlayer fp=new FakePlayer(world(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DistillProbe"));ContainerDistillery c=new ContainerDistillery(fp.field_71071_by,t);fp.field_71071_by.func_70299_a(9,stack("cookie",3));c.func_82846_b(fp,7);check(t.func_70301_a(0)!=null&&t.func_70301_a(0).func_77973_b()==stack("cookie",1).func_77973_b(),"shift-click custom nonsmeltable input");fp.field_71071_by.func_70299_a(10,jars(3));c.func_82846_b(fp,8);check(t.func_70301_a(2)!=null&&t.func_70301_a(2).field_77994_a==3,"shift-click jar slot");for(int slot=3;slot<7;slot++)check(!t.func_102007_a(slot,stack("dirt",1),2),"automation cannot insert in output slot "+slot);
   // Scripted inputs match exact metadata, including ordinary block variants.
   script("Distillery.addRecipe(\"custom:tool\",[<minecraft:wool:1>],[<minecraft:apple>]);");t=tile();ItemStack tool=stack("wool",1);t.func_70299_a(0,tool);check(t.getActiveRecipe()==null,"wrong exact metadata rejected");tool.func_77964_b(1);check(t.getActiveRecipe()!=null,"exact metadata accepted");script("");
   t=tile();DistilleryRecipe nativeRecipe=original.get(0);t.func_70299_a(0,nativeRecipe.inputs[0].func_77946_l());t.func_70299_a(1,nativeRecipe.inputs[1].func_77946_l());t.func_70299_a(2,jars(nativeRecipe.jars));ticks(t,800);check(t.func_70301_a(3).func_77969_a(nativeRecipe.outputs[0]),"untouched native recipe actually completes");
  }finally{PowerSources.instance().removePowerSource(p);}
  org.fentanylsolutions.thaumicdabblery.compat.modtweaker.WitcheryDistilleryZen.dump();
 }
}
