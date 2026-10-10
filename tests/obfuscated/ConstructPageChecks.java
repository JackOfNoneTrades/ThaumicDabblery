package tdtest;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import minetweaker.MineTweakerImplementationAPI;
import net.minecraft.item.*;
import net.minecraft.launchwrapper.Launch;
import thaumcraft.api.aspects.*;
import thaumcraft.api.research.*;
import org.fentanylsolutions.thaumicdabblery.feature.construct.ConstructPage;

public final class ConstructPageChecks {
 public int checks;
 public void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 private void script(String s)throws Exception{Files.write(Paths.get("scripts/zz-construct-checks.zs"),s.getBytes(StandardCharsets.UTF_8));MineTweakerImplementationAPI.reload();}
 private String add(String args){return "mods.thaumcraft.Research.addConstructPage(\"TD_CONSTRUCT_BASE\", "+args+");\n";}
 private String cell="[[[<minecraft:stone>]]]";
 public static ItemStack stack(String id){return new ItemStack((Item)Item.field_150901_e.func_82594_a("minecraft:"+id));}
 public void baseline(){
  ResearchItem r=ResearchCategories.getResearch("TD_PORTAL_CONSTRUCTS");check(r!=null&&r.isAutoUnlock(),"demo exists and unlocks automatically");
  check(r.getPages().length==3,"three demo construct pages");
  ConstructPage n=(ConstructPage)r.getPages()[0],a=(ConstructPage)r.getPages()[1],t=(ConstructPage)r.getPages()[2];
  check(n.width==4&&n.height==5&&n.depth==1,"Nether frame dimensions");check(n.layers[0][0][1].func_77969_a(stack("obsidian"))&&n.layers[1][0][1]==null&&n.layers[4][0][1]!=null,"bottom first and empty frame interior");
  check(n.activation.func_77969_a(stack("flint_and_steel")),"Nether activation");
  check(a.layers[2][0][0].func_77969_a(stack("glowstone"))&&a.activation.func_77969_a(stack("water_bucket")),"Aether material and activation");
  check(t.width==4&&t.depth==4&&t.height==2&&t.activation.func_77969_a(stack("diamond")),"Twilight layout and activation");
  check(t.layers[0][1][1].func_77969_a(stack("water"))&&t.layers[1][1][1]==null,"water beneath empty upper cells");
  check(t.layers[1][0][0].func_77960_j()==32767,"wildcard flowers retained");
  for(ResearchPage p:r.getPages()){check(p.recipeOutput==null,"display does not claim a craftable output");check(((ConstructPage)p).cost.size()==0,"free page has no phantom aspect cost");}
 }
 public void run()throws Exception{
  check(Boolean.FALSE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment")),"obfuscated runtime");baseline();
  ResearchPage[] original={new ResearchPage("original")};ResearchItem base=new ResearchItem("TD_CONSTRUCT_BASE","BASICS",new AspectList(),90,90,0,stack("stone")).setPages(original).registerResearchItem();
  ResearchItem empty=new ResearchItem("TD_CONSTRUCT_EMPTY","BASICS",new AspectList(),92,90,0,stack("stone")).registerResearchItem();
  try{
   script(add(cell)+add(cell+", <minecraft:diamond>")+add(cell+", null, \"aer 10, terra 5, aer 2\""));
   check(base.getPages().length==4&&base.getPages()[0]==original[0],"all overloads append retaining existing pages");
   ConstructPage p=(ConstructPage)base.getPages()[3];check(p.activation==null&&p.cost.getAmount(Aspect.AIR)==12&&p.cost.getAmount(Aspect.EARTH)==5,"optional activation and merged display costs");
   for(int i=0;i<3;i++){MineTweakerImplementationAPI.reload();check(base.getPages().length==4,"reload without duplicates");}
   script("");check(base.getPages()==original,"undo restores exact original array");
   script("mods.thaumcraft.Research.addConstructPage(\"TD_CONSTRUCT_EMPTY\", "+cell+");");check(empty.getPages().length==1,"append to null pages");script("");check(empty.getPages()==null,"undo restores null array");
   script("mods.thaumcraft.Research.clearPages(\"TD_CONSTRUCT_BASE\");\n"+add(cell));check(base.getPages().length==1,"clear then append");script("");check(base.getPages()==original,"clear/append undo");
   script(add(cell)+"mods.thaumcraft.Research.clearPages(\"TD_CONSTRUCT_BASE\");");check(base.getPages().length==0,"append then clear");script("");check(base.getPages()==original,"append/clear undo");
   script(add("[[[<minecraft:stone>.withTag({display:{Name:\"Named Stone\"}})]]]"));
   check(((ConstructPage)base.getPages()[1]).layers[0][0][0].func_82833_r().equals("Named Stone"),"cell NBT retained");
   script(add("null")+add("[]")+add("[[[null]]]")+add("[[[<minecraft:stone>],[<minecraft:stone>,<minecraft:stone>]]]")+add("[[[<minecraft:stone>]],[[<minecraft:stone>],[<minecraft:stone>]]]")+add("[[[<minecraft:stone>*2]]]")+add(cell+", <minecraft:diamond>*2")+add(cell+", null, \"missing 1\"")+add(cell+", null, \"aer -1\"")+add(cell+", null, \"aer 2147483647, aer 1\"")+add(cell+", null, \"aer nope\"")+add(cell+", null, \"aer 1,\"")+"mods.thaumcraft.Research.addConstructPage(\"MISSING\", "+cell+");\n"+"mods.thaumcraft.Research.addConstructPage(\"\", "+cell+");\n"+add(cell));
   check(base.getPages().length==2&&base.getPages()[0]==original[0],"invalid calls are rejected while a following valid call still executes");
   script("");
   script(add("["+String.join(",",Collections.nCopies(17,"[[<minecraft:stone>]]"))+"]"));check(base.getPages()==original,"oversized grids rejected");
   script(add(cell));ResearchPage foreign=new ResearchPage("foreign");base.setPages(original[0],base.getPages()[1],foreign);script("");check(base.getPages().length==2&&base.getPages()[1]==foreign,"undo preserves unrelated changes");base.setPages(original);
   script(add(cell));ResearchItem replacement=new ResearchItem("TD_CONSTRUCT_BASE","BASICS",new AspectList(),90,90,0,stack("stone")).setPages(foreign);ResearchCategories.getResearchList("BASICS").research.put(base.key,replacement);script("");check(base.getPages()==original&&replacement.getPages()[0]==foreign,"undo targets original owner, not replacement");ResearchCategories.getResearchList("BASICS").research.put(base.key,base);
   script(add(cell)+"mods.thaumcraft.Research.removeResearch(\"TD_CONSTRUCT_BASE\");");check(ResearchCategories.getResearch(base.key)==null,"research removal");script("");check(ResearchCategories.getResearch(base.key)==base&&base.getPages()==original,"research removal undo");
   Path demo=Paths.get("scripts/construct-pages.zs");byte[] text=Files.readAllBytes(demo);try{Files.delete(demo);MineTweakerImplementationAPI.reload();check(ResearchCategories.getResearch("TD_PORTAL_CONSTRUCTS")==null,"removing demo removes pages and research");}finally{Files.write(demo,text);MineTweakerImplementationAPI.reload();}baseline();
  }finally{script("");}
 }
}
