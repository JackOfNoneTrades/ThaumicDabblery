package tdtest;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.mojang.authlib.GameProfile;
import com.kentington.thaumichorizons.common.ThaumicHorizons;
import com.kentington.thaumichorizons.common.lib.*;
import com.kentington.thaumichorizons.common.tiles.TileVat;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.*;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import minetweaker.MineTweakerImplementationAPI;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.CreatureInfusionZen;
import thaumcraft.api.aspects.*;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.research.*;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.tiles.TilePedestal;

/** Production/SRG names deliberately catch missing obfuscation mappings. Run only in a disposable world. */
public final class CreatureInfusionChecks {
 public int checks;
 public void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 public static Object field(Object o,String name)throws Exception{Field f=(o instanceof Class?(Class<?>)o:o.getClass()).getDeclaredField(name);f.setAccessible(true);return f.get(o instanceof Class?null:o);}
 public static ItemStack stack(String name){return new ItemStack((Item)Item.field_150901_e.func_82594_a("minecraft:"+name),1,0);}
 public static String set(String key,String research,int instability,String aspects,String items){return "mods.thaumichorizons.CreatureInfusion.setRecipe(\""+key+"\",\""+research+"\","+instability+",\""+aspects+"\",["+items+"]);\n";}
 public static String remove(String key){return "mods.thaumichorizons.CreatureInfusion.removeRecipe(\""+key+"\");\n";}
 public void script(String source)throws Exception{Files.createDirectories(Paths.get("scripts"));Files.write(Paths.get("scripts/zz-creature-test.zs"),source.getBytes(StandardCharsets.UTF_8));MineTweakerImplementationAPI.reload();}
 public Map<String,Object> entries()throws Exception{return (Map<String,Object>)field(CreatureInfusionZen.class,"ENTRIES");}
 public CreatureInfusionRecipe recipe(String key)throws Exception{return (CreatureInfusionRecipe)field(entries().get(key),"current");}
 private final Map<ResearchItem,ResearchPage[]> originalPages=new IdentityHashMap<>();
 private List<CreatureInfusionRecipe> originalRecipes;
 public void restored()throws Exception{
  check(ThaumicHorizons.critterRecipes.equals(originalRecipes),"exact recipe objects and priority restored");
  for(Map.Entry<ResearchItem,ResearchPage[]> e:originalPages.entrySet())check(e.getKey().getPages()==e.getValue(),"exact original page array restored for "+e.getKey().key);
 }
 private int verifyPages(String key,boolean removed,int cost)throws Exception{
  int count=0;
  for(Object slot:(List<?>)field(CreatureInfusionZen.class,"SLOTS")){
   List<?> linked=Arrays.asList((Object[])field(slot,"entries"));if(!linked.contains(entries().get(key)))continue;count++;
   ResearchItem owner=(ResearchItem)field(slot,"research");ResearchPage current=(ResearchPage)field(slot,"current");
   if(removed){check(current==null&&!Arrays.asList(owner.getPages()).contains(field(slot,"original")),"native recipe page removed: "+key);continue;}
   check(current!=null&&Arrays.asList(owner.getPages()).contains(current),"display remains attached: "+key);
   InfusionRecipe[] recipes=current.recipe instanceof InfusionRecipe?new InfusionRecipe[]{(InfusionRecipe)current.recipe}:(InfusionRecipe[])current.recipe;
   boolean match=false;for(InfusionRecipe r:recipes)if(r.getAspects().getAmount(Aspect.FIRE)==cost){match=true;check(r.getInstability()==3,"display instability");check(r.getComponents().length==1&&r.getComponents()[0].func_77973_b()==stack("cookie").func_77973_b(),"display ingredients");}
   check(match,"display essentia updated: "+key);
  }
  check(count>0,"native display located: "+key);return count;
 }
 public void run(WorldServer world)throws Exception{
  check(Boolean.FALSE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment")),"production environment");
  script("");CreatureInfusionZen.initialize();originalRecipes=new ArrayList<>(ThaumicHorizons.critterRecipes);
  for(ResearchCategoryList c:ResearchCategories.researchCategories.values())for(ResearchItem r:c.research.values())originalPages.put(r,r.getPages());
  check(entries().size()==30,"all thirty native creature recipes indexed, got "+entries().size());
  System.out.println("TD_CREATURE_KEYS "+entries().keySet());
  EntityPlayerMP player=FakePlayerFactory.get(world,new GameProfile(UUID.randomUUID(),"CreatureProbe"));String name=player.func_70005_c_();
  Thaumcraft.proxy.getPlayerKnowledge().researchCompleted.put(name,new ArrayList<String>());
  try{
   for(String key:entries().keySet()){
    CreatureInfusionRecipe before=recipe(key);int index=ThaumicHorizons.critterRecipes.indexOf(before);
    script(set(key,"",3,"ignis 2, aer 1, ignis 3","<minecraft:cookie> * 5"));CreatureInfusionRecipe after=recipe(key);
    check(after!=before&&ThaumicHorizons.critterRecipes.indexOf(after)==index,"edited in place: "+key);
    check(after.getID(null)==before.getID(null)&&after.getRecipeInput()==before.getRecipeInput(),"effect and input preserved");
    check(after.getRecipeOutput().equals(before.getRecipeOutput()),"output payload preserved");
    if(before.getRecipeOutput() instanceof NBTBase)check(after.getRecipeOutput()!=before.getRecipeOutput(),"output NBT defensively copied");
    check(after.getComponents()[0].field_77994_a==1&&after.getAspects().getAmount(Aspect.FIRE)==5,"costs and per-pedestal stack size");
    verifyPages(key,false,5);
    script(remove(key));check(!ThaumicHorizons.critterRecipes.contains(before)&&recipe(key)==null,"removed native recipe");verifyPages(key,true,0);
    script("");restored();
   }
   String edit=set("upgrade:4","",3,"ignis 5","<minecraft:cookie>");
   script(remove("upgrade:4")+remove("upgrade:4")+edit);check(ThaumicHorizons.critterRecipes.indexOf(recipe("upgrade:4"))==originalRecipes.indexOf((CreatureInfusionRecipe)field(entries().get("upgrade:4"),"original")),"remove then set restores priority");verifyPages("upgrade:4",false,5);
   script(edit+remove("upgrade:4")+edit+set("upgrade:4","",3,"ignis 5","<minecraft:cookie>"));script("");restored();
   script(remove("upgrade:4")+"mods.thaumcraft.Research.clearPages(\"infusionVat\");\n"+edit);check(ResearchCategories.getResearch("infusionVat").getPages().length==0,"redefinition does not resurrect deliberately cleared pages");script("");restored();
   script(set("upgrade:4","",0,"","<minecraft:cookie>, <minecraft:cookie>"));check(recipe("upgrade:4").getAspects().size()==0&&recipe("upgrade:4").getComponents().length==2,"zero essentia and repeated pedestals supported");script("");restored();
   StringBuilder all=new StringBuilder();for(String key:entries().keySet())all.append(remove(key));script(all.toString());check(ThaumicHorizons.critterRecipes.isEmpty(),"all native recipes removed together");script("");restored();
   for(String invalid:new String[]{set("bogus","",3,"ignis 1","<minecraft:cookie>"),set("upgrade:4","missingResearch",3,"ignis 1","<minecraft:cookie>"),set("upgrade:4","",-1,"ignis 1","<minecraft:cookie>"),set("upgrade:4","",3,"unknown 1","<minecraft:cookie>"),set("upgrade:4","",3,"ignis 0","<minecraft:cookie>"),set("upgrade:4","",3,"ignis -1","<minecraft:cookie>"),set("upgrade:4","",3,"ignis 2147483647, ignis 1","<minecraft:cookie>"),set("upgrade:4","",3,"ignis 1,","<minecraft:cookie>"),set("upgrade:4","",3,"ignis 1",""),remove("upgrade:999")}){script(invalid);restored();}script("");
   script(set("upgrade:4","infusionVat",3,"ignis 5","<minecraft:cookie>"));EntityCow cow=new EntityCow(world);
   ArrayList<ItemStack> parts=new ArrayList<>(Arrays.asList(stack("cookie")));
   check(ThaumicHorizons.getCreatureInfusion(cow,parts,player)==null,"research requirement enforced");ResearchManager.completeResearchUnsaved(name,"infusionVat");
   check(ThaumicHorizons.getCreatureInfusion(cow,parts,player)==recipe("upgrade:4"),"completed research permits new ingredients");
   check(ThaumicHorizons.getCreatureInfusion(cow,new ArrayList<>(Arrays.asList(stack("cookie"),stack("cookie"))),player)==null,"extra pedestal prevents match");
   script(edit);vat(world,player,"upgrade:4",false);
   script(set("transform:Cow->ThaumicHorizons.ChocolateCow","",3,"ignis 5","<minecraft:cookie>"));
   check(ThaumicHorizons.getCreatureInfusion(new EntityPig(world),parts,player)==null,"transformation keeps input creature restriction");vat(world,player,"transform:Cow->ThaumicHorizons.ChocolateCow",true);
   script("");restored();
   for(String op:new String[]{"mods.thaumcraft.Research.clearPages(\"infusionVat\");\n","mods.thaumcraft.Research.removeResearch(\"infusionVat\");\n"}){
    script(op+edit);script("");restored();script(edit+op);script("");restored();
   }
   if(cpw.mods.fml.common.FMLCommonHandler.instance().getSide().isServer()){MinecraftServer.func_71276_C().func_71187_D().func_71556_a(MinecraftServer.func_71276_C(),"mt creatureInfusions");
   String log=new String(Files.readAllBytes(Paths.get("logs/minetweaker.log")),StandardCharsets.UTF_8);check(log.contains("transform:Cow->ThaumicHorizons.ChocolateCow")&&log.contains("upgrade:4 [active]"),"command logs stable keys and active state");}
  }finally{script("");}
 }
 private void vat(WorldServer world,EntityPlayerMP player,String key,boolean transform)throws Exception{
  TileVat vat=new TileVat();vat.func_145834_a(world);vat.field_145851_c=0;vat.field_145848_d=100;vat.field_145849_e=0;
  world.func_147465_d(2,99,0,ConfigBlocks.blockStoneDevice,1,3);TilePedestal pedestal=(TilePedestal)world.func_147438_o(2,99,0);pedestal.func_70299_a(0,stack("cookie"));
  EntityCow cow=new EntityCow(world);vat.setEntityContained(cow);vat.startInfusion(player);
  check(vat.mode==2,"real vat starts edited recipe "+key);check(((AspectList)field(vat,"essentiaDemanded")).getAmount(Aspect.FIRE)==5,"vat captures edited essentia");check((Integer)field(vat,"recipeInstability")==3,"vat captures edited instability");
  Object output=field(vat,"recipeOutput");script("");check(((AspectList)field(vat,"essentiaDemanded")).getAmount(Aspect.FIRE)==5,"active vat cost survives script undo");
  vat.craftingFinish(output,"");
  if(transform)check("ThaumicHorizons.ChocolateCow".equals(EntityList.func_75621_b(vat.getEntityContained())),"native transformation completes");
  else check(vat.getEntityContained()==cow&&((EntityInfusionProperties)cow.getClass().getMethod("getExtendedProperties",String.class).invoke(cow,"CreatureInfusion")).hasInfusion(4),"native upgrade completes without replacing creature");
  check(vat.mode==0,"vat completion returns to idle");world.func_147468_f(2,99,0);
 }
}
