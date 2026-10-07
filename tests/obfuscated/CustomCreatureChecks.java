package tdtest;

import java.util.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import com.mojang.authlib.GameProfile;
import com.kentington.thaumichorizons.common.ThaumicHorizons;
import com.kentington.thaumichorizons.common.lib.*;
import com.kentington.thaumichorizons.common.tiles.*;
import net.minecraft.entity.*;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.world.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.CreatureInfusionZen;
import org.fentanylsolutions.thaumicdabblery.compat.thaumichorizons.CustomCreatureRecipe;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.*;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.research.*;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.tiles.TilePedestal;

public final class CustomCreatureChecks {
 public final CreatureInfusionChecks base=new CreatureInfusionChecks();
 public static final String RESEARCH="TD_CUSTOM_CREATURE";
 public int checks;
 public void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static Object get(Object o,String n)throws Exception{return CreatureInfusionChecks.field(o,n);}
 public static void put(Object o,String n,Object v)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 public static ItemStack stack(String n){return CreatureInfusionChecks.stack(n);}
 public static String add(String key,String research,String input,String output,String item){return "mods.thaumichorizons.CreatureInfusion.addRecipe(\""+key+"\",\""+research+"\",\""+input+"\",\""+output+"\",2,\"exanimis 8\",[<minecraft:"+item+">]);\n";}
 public static String page(String key){return "mods.thaumichorizons.CreatureInfusion.addPage(\""+RESEARCH+"\",\""+key+"\",<minecraft:spawn_egg:90>.withTag({display:{Name:\"Living pig\"}}),<minecraft:spawn_egg:57>.withTag({display:{Name:\"Undead pig\"}}));\n";}
 public static String demo(){return add("custom:pigman",RESEARCH,"Pig","PigZombie","rotten_flesh")+page("custom:pigman");}
 public static void setup(){
  ResearchCategories.registerCategory(RESEARCH,new ResourceLocation("thaumcraft","textures/aspects/exanimis.png"),new ResourceLocation("thaumcraft","textures/gui/gui_researchback.png"));
  new ResearchItem(RESEARCH,RESEARCH,new AspectList().add(Aspect.UNDEAD,1),0,0,1,stack("rotten_flesh")).setAutoUnlock().setPages(new ResearchPage("Custom creature recipe fixture")).registerResearchItem();
 }
 public CreatureInfusionRecipe current(String key)throws Exception{return base.recipe(key);}
 public EntityInfusionProperties props(EntityLivingBase e)throws Exception{return (EntityInfusionProperties)e.getClass().getMethod("getExtendedProperties",String.class).invoke(e,"CreatureInfusion");}
 private TileVat vat(WorldServer world,EntityLiving source,String item){
  TileVat vat=new TileVat();vat.func_145834_a(world);vat.field_145851_c=0;vat.field_145848_d=100;vat.field_145849_e=0;vat.setEntityContained(source);
  world.func_147465_d(2,99,0,ConfigBlocks.blockStoneDevice,1,3);((TilePedestal)world.func_147438_o(2,99,0)).func_70299_a(0,stack(item));return vat;
 }
 public void start(TileVat vat,WorldServer world,EntityPlayerMP player){new TileVatMatrix(){@Override public TileVat getVat(){return vat;}}.onWandRightClick(world,null,player);}
 public void complete(TileVat vat,WorldServer world)throws Exception{
  vat.instability=0;
  AspectList demand=(AspectList)get(vat,"essentiaDemanded");for(Aspect aspect:demand.getAspects())if(aspect!=null&&demand.getAmount(aspect)>0)vat.addToContainer(aspect,demand.getAmount(aspect));
  for(int i=0;i<30&&vat.mode==2;i++)vat.craftCycle();
  check(vat.mode==0,"native crafting cycle completes");check(((TilePedestal)world.func_147438_o(vat.field_145851_c+2,vat.field_145848_d-1,vat.field_145849_e)).func_70301_a(0)==null,"pedestal ingredient actually consumed");
 }
 private void pages(int expected,int cost)throws Exception{
  ResearchPage[] pages=ResearchCategories.getResearch(RESEARCH).getPages();check(pages.length==expected,"custom page count "+expected);
  if(expected>1){InfusionRecipe r=(InfusionRecipe)pages[1].recipe;check(r.getRecipeInput().func_77960_j()==90&&((ItemStack)r.getRecipeOutput()).func_77960_j()==57,"independent display metadata");check("Living pig".equals(r.getRecipeInput().func_82833_r())&&"Undead pig".equals(((ItemStack)r.getRecipeOutput()).func_82833_r()),"display NBT names preserved");check(r.getAspects().getAmount(Aspect.UNDEAD)==cost,"display costs synchronized");check(!ThaumcraftApi.getCraftingRecipes().contains(r),"display is not a real item infusion recipe");}
 }
 public void run(WorldServer world)throws Exception{
  base.run(world);setup();base.script("");ResearchPage[] before=ResearchCategories.getResearch(RESEARCH).getPages();int natives=ThaumicHorizons.critterRecipes.size(),slots=((List<?>)get(CreatureInfusionZen.class,"SLOTS")).size();
  EntityPlayerMP player=FakePlayerFactory.get(world,new GameProfile(UUID.randomUUID(),"CustomCreatureProbe"));String name=player.func_70005_c_();thaumcraft.common.Thaumcraft.proxy.getPlayerKnowledge().researchCompleted.put(name,new ArrayList<String>());
  try{
   base.script(demo());check(current("custom:pigman") instanceof CustomCreatureRecipe,"custom native recipe subclass registered");check(ThaumicHorizons.critterRecipes.indexOf(current("custom:pigman"))==natives,"new recipe appended after native priorities");pages(2,8);
   TileVat vat=vat(world,new EntityPig(world),"rotten_flesh");start(vat,world,player);check(vat.mode==0,"research requirement prevents starting");ResearchManager.completeResearchUnsaved(name,RESEARCH);start(vat,world,player);check(vat.mode==2,"wand starts custom recipe after research");
   EntityPig source=(EntityPig)vat.getEntityContained();source.func_94058_c("Bacon");source.func_70606_j(3);props(source).addInfusion(4);
   NBTTagCompound saved=new NBTTagCompound();vat.func_145841_b(saved);CompressedStreamTools.func_74795_b(saved,Paths.get("pending-custom-vat.dat").toFile());
   base.script("");check(!base.entries().containsKey("custom:pigman")&&ResearchCategories.getResearch(RESEARCH).getPages()==before,"script undo removes registration and exact page array");
   TileVat loaded=new TileVat();loaded.func_145834_a(world);loaded.func_145839_a(saved);loaded.func_145845_h();check(loaded.mode==2&&loaded.getEntityContained() instanceof EntityPig,"in-progress vat and input restored from NBT");complete(loaded,world);
   check(loaded.getEntityContained() instanceof EntityPigZombie,"pig becomes vanilla zombie pigman");check(loaded.getEntityContained().func_110138_aP()==20&&loaded.getEntityContained().func_110138_aP()!=source.func_110138_aP(),"output keeps its own species health");check("Bacon".equals(((EntityLiving)loaded.getEntityContained()).func_94057_bL()),"custom name retained");check(!props(loaded.getEntityContained()).hasInfusion(4),"source infusion is not copied onto new species");
   check(props(loaded.getEntityContained()).getInfusionCosts().getAmount(Aspect.UNDEAD)==8,"new transformation cost recorded");
   // A subsequent native NBT upgrade must not reuse the custom transformation label.
   base.script(CreatureInfusionChecks.set("upgrade:4","",0,"","<minecraft:cookie>"));loaded.setEntityContained(new EntityPig(world));((TilePedestal)world.func_147438_o(2,99,0)).func_70299_a(0,stack("cookie"));start(loaded,world,player);check("".equals(get(loaded,"recipeOutputLabel")),"next native recipe clears transformation label");complete(loaded,world);check(props(loaded.getEntityContained()).hasInfusion(4),"native upgrade still finishes after custom transformation in same vat");
   base.script(demo()+CreatureInfusionChecks.set("custom:pigman",RESEARCH,1,"exanimis 3","<minecraft:cookie>"));pages(2,3);check(current("custom:pigman") instanceof CustomCreatureRecipe,"setRecipe preserves transformation behavior");
   base.script(demo()+CreatureInfusionChecks.remove("custom:pigman"));pages(1,0);check(current("custom:pigman")==null,"removal disables custom recipe");
   base.script(demo()+CreatureInfusionChecks.remove("custom:pigman")+CreatureInfusionChecks.set("custom:pigman",RESEARCH,1,"exanimis 3","<minecraft:cookie>"));pages(2,3);
   String pair=add("custom:a","","Pig","PigZombie","rotten_flesh")+add("custom:b","","Pig","PigZombie","cookie");base.script(pair);check(ThaumicHorizons.getCreatureInfusion(new EntityPig(world),new ArrayList<>(Arrays.asList(stack("cookie"))),player)==current("custom:b"),"two recipes for same mob pair");
   base.script(pair+CreatureInfusionChecks.remove("custom:a")+CreatureInfusionChecks.set("custom:a","",0,"","<minecraft:rotten_flesh>"));check(ThaumicHorizons.critterRecipes.indexOf(current("custom:a"))<ThaumicHorizons.critterRecipes.indexOf(current("custom:b")),"redefinition preserves custom registration order");
   base.script(CreatureInfusionChecks.remove("upgrade:10")+add("custom:priority","","Pig","PigZombie","cookie")+CreatureInfusionChecks.set("upgrade:10","",0,"","<minecraft:cookie>"));check(ThaumicHorizons.critterRecipes.indexOf(current("upgrade:10"))<ThaumicHorizons.critterRecipes.indexOf(current("custom:priority")),"restored final native recipe precedes appended custom recipes");
   base.script(add("custom:unstable","","Pig","PigZombie","cookie").replace(",2,",",34,"));check(!base.entries().containsKey("custom:unstable"),"unsafe native instability bound rejected");
   base.script(demo()+page("custom:pigman")+CreatureInfusionChecks.set("custom:pigman",RESEARCH,1,"exanimis 3","<minecraft:cookie>"));pages(3,3);check(((InfusionRecipe)ResearchCategories.getResearch(RESEARCH).getPages()[2].recipe).getAspects().getAmount(Aspect.UNDEAD)==3,"all linked pages update");
   base.script(demo()+"mods.thaumichorizons.CreatureInfusion.addPage(\""+RESEARCH+"\",\"custom:pigman\",<minecraft:spawn_egg:*>,<minecraft:spawn_egg:57>);\n");pages(2,8);
   base.script(add("custom:undead","","Zombie","Pig","cookie")+CreatureInfusionChecks.set("upgrade:4","",0,"","<minecraft:rotten_flesh>"));TileVat undead=vat(world,new EntityZombie(world),"rotten_flesh");check(undead.isValidInfusionTarget(),"explicit custom input permits undead vat use");start(undead,world,player);check(undead.mode==0,"undead cannot start native upgrades");
   undead.setEntityContained(new EntityPigZombie(world));check(!undead.isValidInfusionTarget(),"exact Zombie input excludes PigZombie subclass");undead.setEntityContained(new EntityZombie(world));((TilePedestal)world.func_147438_o(2,99,0)).func_70299_a(0,stack("cookie"));start(undead,world,player);check(undead.mode==2,"undead custom recipe starts through wand gate");complete(undead,world);check(undead.getEntityContained() instanceof EntityPig,"undead custom input transforms");base.script("");undead.setEntityContained(new EntityZombie(world));check(!undead.isValidInfusionTarget(),"reload restores native undead restriction");
   for(String[] pairIds:new String[][]{{"Pig","ThaumicHorizons.ChocolateCow"},{"ThaumicHorizons.ChocolateCow","Pig"}}){base.script(add("custom:modded","",pairIds[0],pairIds[1],"cookie"));TileVat modded=vat(world,(EntityLiving)EntityList.func_75620_a(pairIds[0],world),"cookie");start(modded,world,player);check(modded.mode==2,"modded input/output recipe starts");complete(modded,world);check(pairIds[1].equals(EntityList.func_75621_b(modded.getEntityContained())),"modded transformation output");}
   for(String[] ids:new String[][]{{"Missing","Pig"},{"Pig","Missing"},{"Pig","Item"},{"Villager","Pig"},{"SnowMan","Pig"}}){base.script(add("custom:bad","",ids[0],ids[1],"cookie"));check(!base.entries().containsKey("custom:bad")&&ThaumicHorizons.critterRecipes.size()==natives,"invalid entities rejected");}
   base.script(demo()+demo());check(ThaumicHorizons.critterRecipes.size()==natives+1,"duplicate key cannot create competing registration");
   for(String op:new String[]{"mods.thaumcraft.Research.clearPages(\""+RESEARCH+"\");\n","mods.thaumcraft.Research.removeResearch(\""+RESEARCH+"\");\n"}){base.script(demo()+op);base.script("");check(ResearchCategories.getResearch(RESEARCH).getPages()==before,"page/research edits undo together");}
   base.script(demo()+CreatureInfusionChecks.remove("custom:pigman")+"mods.thaumcraft.Research.clearPages(\""+RESEARCH+"\");\n"+CreatureInfusionChecks.set("custom:pigman","",0,"","<minecraft:cookie>"));check(ResearchCategories.getResearch(RESEARCH).getPages().length==0,"custom redefinition respects cleared pages");
   base.script(add("custom:failure","","Pig","PigZombie","cookie"));TileVat failure=vat(world,new EntityPig(world),"cookie");start(failure,world,player);EntityLivingBase retained=failure.getEntityContained();((NBTTagCompound)get(failure,"recipeOutput")).func_74778_a("entity","MissingAfterSave");complete(failure,world);check(failure.getEntityContained()==retained,"unavailable saved output preserves source without crashing");
   new CreatureBreachChecks(this,world).run();
   base.script("");check(base.entries().size()==30&&ThaumicHorizons.critterRecipes.size()==natives&&((List<?>)get(CreatureInfusionZen.class,"SLOTS")).size()==slots,"reload leaves no custom registry or page slots");check(((Map<?,?>)get(CreatureInfusionZen.class,"MANAGED_PAGES")).isEmpty(),"reload releases page snapshots");
  }finally{base.script("");world.func_147468_f(2,99,0);checks+=base.checks;}
 }
}
