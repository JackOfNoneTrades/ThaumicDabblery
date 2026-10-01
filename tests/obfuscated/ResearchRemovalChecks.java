package tdtest;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;
import minetweaker.MineTweakerImplementationAPI;
import modtweaker2.mods.thaumcraft.research.OrphanResearch;
import thaumcraft.api.aspects.*;
import thaumcraft.api.research.*;

public final class ResearchRemovalChecks {
 public int checks;
 public static final String REMOVED="TD_REMOVED",SURVIVES="TD_SURVIVES",SECOND="TD_SECOND";
 private final Map<String,ResearchItem> items=new LinkedHashMap<>();
 private final Map<String,String[][]> originals=new LinkedHashMap<>();
 public void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 private static String[] copy(String[] values){return values==null?null:values.clone();}
 public static String command(String method,String key){return "mods.thaumcraft.Research."+method+"(\""+key+"\");\n";}
 public void script(String source)throws Exception{
  Files.createDirectories(Paths.get("scripts"));
  Files.write(Paths.get("scripts/zz-research-removal-test.zs"),source.getBytes(StandardCharsets.UTF_8));
  MineTweakerImplementationAPI.reload();
 }
 private void category(String key){ResearchCategories.registerCategory(key,new ResourceLocation("thaumcraft","textures/aspects/ordo.png"),new ResourceLocation("thaumcraft","textures/gui/gui_researchback.png"));}
 private ResearchItem research(String key,String tab,int column){
  ResearchItem r=new ResearchItem(key,tab,new AspectList().add(Aspect.ORDER,1),column,0,1,new ResourceLocation("thaumcraft","textures/aspects/ordo.png"));
  r.setAutoUnlock();r.setPages(new ResearchPage("Removal regression fixture"));r.registerResearchItem();items.put(key,r);return r;
 }
 private void setup()throws Exception{
  category(REMOVED);category(SURVIVES);category(SECOND);category("TD_EMPTY");
  ResearchItem a=research("TD_A",REMOVED,0),b=research("TD_B",REMOVED,2),keep=research("TD_KEEP",SURVIVES,0),other=research("TD_OTHER",SURVIVES,2),c=research("TD_C",SECOND,0);
  a.setSiblings("TD_B","TD_B");b.setParents("TD_A","TD_A").setParentsHidden("TD_A");
  c.setParents("TD_A","TD_A").setSiblings("TD_B","TD_B");
  other.setParents("TD_A","TD_KEEP","TD_A","TD_B","TD_C","TD_B")
   .setParentsHidden("TD_B","TD_A","TD_B","TD_A")
   .setSiblings("TD_A","TD_B","TD_KEEP","TD_A","TD_B","TD_C");
  keep.setParentsHidden(new String[0]);
  // Addon setters can reject null (WGResearchItem) or rewrite already stored keys.
  // Exercise both untouched null lists and modified lists without invoking those setters.
  for(int i=0;i<2;i++){
   ResearchItem guarded=new ResearchItem("TD_GUARDED_"+i,SURVIVES,new AspectList(),10+i*2,0,1,new ResourceLocation("thaumcraft","textures/aspects/ordo.png")){
    @Override public ResearchItem setParents(String... keys){throw new AssertionError("addon parent setter called");}
    @Override public ResearchItem setParentsHidden(String... keys){throw new AssertionError("addon hidden-parent setter called");}
    @Override public ResearchItem setSiblings(String... keys){throw new AssertionError("addon sibling setter called");}
   };
   if(i==1){guarded.parents=new String[]{"TD_A","TD_KEEP","TD_A"};guarded.parentsHidden=new String[]{"TD_B","TD_A","TD_B"};guarded.siblings=new String[]{"TD_A","TD_B","TD_KEEP"};}
   guarded.setPages(new ResearchPage("Addon setter regression"));guarded.registerResearchItem();items.put(guarded.key,guarded);
  }
  if(cpw.mods.fml.common.Loader.isModLoaded("WitchingGadgets")){
   Class<?> type=Class.forName("witchinggadgets.common.util.research.WGResearchItem");
   for(int i=0;i<2;i++){
    ResearchItem wg=(ResearchItem)type.getConstructor(String.class,String.class,AspectList.class,int.class,int.class,int.class,ResourceLocation.class)
     .newInstance("TD_WG_"+i,SURVIVES,new AspectList(),16+i*2,0,1,new ResourceLocation("thaumcraft","textures/aspects/ordo.png"));
    wg.parents=new String[]{"TD_KEEP"};
    if(i==1){wg.parents=new String[]{"TD_A","TD_KEEP","TD_A"};wg.parentsHidden=new String[]{"TD_B","TD_A","TD_B"};wg.siblings=new String[]{"TD_A","TD_B"};}
    wg.setPages(new ResearchPage("Real Witching Gadgets regression"));wg.registerResearchItem();items.put(wg.key,wg);
   }
  }
  for(ResearchItem r:items.values())originals.put(r.key,new String[][]{copy(r.parents),copy(r.parentsHidden),copy(r.siblings)});
 }
 private String[] filtered(String[] values,Set<String> removed){return values==null?null:Arrays.stream(values).filter(v->!removed.contains(v)).toArray(String[]::new);}
 public void verifyDetached(String... keys){
  Set<String> removed=new HashSet<>(Arrays.asList(keys));
  for(ResearchItem r:items.values()){
   String[][] before=originals.get(r.key);String[][] actual={r.parents,r.parentsHidden,r.siblings};
   for(int i=0;i<3;i++)check(Arrays.equals(actual[i],filtered(before[i],removed)),"all occurrences removed with unrelated order preserved: "+r.key+"/"+i);
  }
 }
 public void verifyRestored(){
  for(ResearchItem r:items.values()){
   check(ResearchCategories.getResearch(r.key)==r,"research identity restored: "+r.key);
   String[][] before=originals.get(r.key);String[][] actual={r.parents,r.parentsHidden,r.siblings};
   for(int i=0;i<3;i++)check(Arrays.equals(actual[i],before[i]),"exact order, duplicates and null/empty state restored: "+r.key+"/"+i);
  }
 }
 public void run()throws Exception{
  check(Boolean.FALSE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment")),"production environment");
  // Clear the previous run's script before registering this process's fixtures.
  script("");setup();script("");verifyRestored();
  try{
   // The reported first failing call must not touch an unrelated addon's null lists or abort the next move.
   script(command("orphanResearch","TKFAKECRUCIBLE")+"mods.thaumcraft.Research.moveResearch(\"TD_OTHER\", \"TD_SURVIVES\", 4, 4);\n");
   check(items.get("TD_OTHER").displayColumn==4&&items.get("TD_OTHER").displayRow==4,"script continues after reported orphanResearch call");
   script("");verifyRestored();
   for(int cycle=0;cycle<3;cycle++){
    script(command("removeTab",REMOVED));
    check(ResearchCategories.getResearchList(REMOVED)==null&&ResearchCategories.getResearch("TD_A")==null,"tab and research lookup removed");
    verifyDetached("TD_A","TD_B");script("");verifyRestored();
   }
   script(command("removeResearch","TD_A"));
   check(ResearchCategories.getResearch("TD_A")==null&&ResearchCategories.getResearch("TD_B")!=null,"individual research removal");
   verifyDetached("TD_A");script("");verifyRestored();
   script(command("orphanResearch","TD_A"));check(ResearchCategories.getResearch("TD_A")!=null,"orphan keeps research");
   verifyDetached("TD_A");script("");verifyRestored();
   script(command("removeTab",REMOVED)+command("removeTab",SECOND));
   check(ResearchCategories.getResearchList(SECOND)==null,"second tab removed");verifyDetached("TD_A","TD_B","TD_C");
   script("");verifyRestored();
   script(command("removeTab",REMOVED)+command("removeResearch","TD_OTHER"));
   check(ResearchCategories.getResearch("TD_OTHER")==null,"dependent removed later in script");script("");verifyRestored();
   script(command("removeTab",REMOVED)+"mods.thaumcraft.Research.moveResearch(\"TD_OTHER\", \"TD_SECOND\", 4, 4);\n");
   check(SECOND.equals(items.get("TD_OTHER").category),"dependent moved later in script");script("");verifyRestored();
   script(command("removeTab","TD_EMPTY"));check(ResearchCategories.getResearchList("TD_EMPTY")==null,"empty tab removed");
   script("");check(ResearchCategories.getResearchList("TD_EMPTY")!=null,"empty tab restored");
   script(command("removeTab","TD_MISSING"));script("");verifyRestored();
   OrphanResearch noop=new OrphanResearch("TD_MISSING");noop.apply();check(!noop.canUndo(),"unchanged orphan has no undo state");
   ResearchItem other=items.get("TD_OTHER");String[] prior=other.parents;
   OrphanResearch orphan=new OrphanResearch("TD_A");orphan.apply();check(orphan.canUndo(),"replacement cleanup exposes undo state");
   prior[0]="MUTATED_OLD_ARRAY";orphan.undo();verifyRestored();
   other.parents[0]="MUTATED_RESTORED_ARRAY";orphan.undo();verifyRestored();
   orphan.apply();verifyDetached("TD_A");orphan.undo();verifyRestored();
   // Missing dependents must not prevent other links from being restored.
   orphan.apply();ResearchCategories.getResearchList(SURVIVES).research.remove("TD_OTHER");orphan.undo();
   check(Arrays.equals(items.get("TD_B").parents,originals.get("TD_B")[0]),"remaining dependent restored when another is missing");
   ResearchCategories.getResearchList(SURVIVES).research.put("TD_OTHER",other);orphan.undo();verifyRestored();
  }finally{script("");}
 }
}
