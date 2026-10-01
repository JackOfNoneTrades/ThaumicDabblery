package tdtest;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import minetweaker.MineTweakerImplementationAPI;
import modtweaker2.mods.thaumcraft.research.AddPrereq;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;
import thaumcraft.api.aspects.*;
import thaumcraft.api.research.*;

public final class ResearchPrerequisiteChecks {
 public int checks;
 private ResearchItem child,empty;
 private final String[] normal={"TD_KEEP","TD_PARENT","TD_PARENT","TD_OTHER","TD_KEEP"};
 private final String[] hidden={"TD_OTHER","TD_PARENT","TD_PARENT","TD_KEEP"};
 private final String[] siblings={"TD_PARENT","TD_OTHER"};
 private void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 private void script(String source)throws Exception{
  Files.createDirectories(Paths.get("scripts"));
  Files.write(Paths.get("scripts/prerequisites.zs"),source.getBytes(StandardCharsets.UTF_8));
  MineTweakerImplementationAPI.reload();
 }
 private String add(String child,String parent,boolean hide){return "mods.thaumcraft.Research.addPrereq(\""+child+"\", \""+parent+"\", "+hide+");\n";}
 private String remove(String child,String parent){return "mods.thaumcraft.Research.removePrereq(\""+child+"\", \""+parent+"\");\n";}
 private ResearchItem make(String key,int x){return new ResearchItem(key,"TD_PREREQS",new AspectList().add(Aspect.ORDER,1),x,0,1,new ResourceLocation("thaumcraft","textures/aspects/ordo.png")).registerResearchItem();}
 private void links(ResearchItem r,String[] parents,String[] hiddenParents,String message){
  check(Arrays.equals(r.parents,parents),message+" normal");check(Arrays.equals(r.parentsHidden,hiddenParents),message+" hidden");
 }
 private void restored()throws Exception{
  script("");links(child,normal,hidden,"exact rollback including duplicates and order");
  links(empty,null,new String[0],"null and empty rollback");
  check(Arrays.equals(child.siblings,siblings),"siblings unchanged");
 }
 public void run()throws Exception{
  check(Boolean.FALSE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment")),"production runtime");
  script("");
  ResourceLocation icon=new ResourceLocation("thaumcraft","textures/aspects/ordo.png"),back=new ResourceLocation("thaumcraft","textures/gui/gui_researchback.png");
  ResearchCategories.registerCategory("TD_PREREQS",icon,back);ResearchCategories.registerCategory("TD_PREREQS_OTHER",icon,back);
  child=make("TD_CHILD",0);empty=make("TD_EMPTY",2);make("TD_PARENT",4);make("TD_KEEP",6);make("TD_OTHER",8);
  child.parents=normal.clone();child.parentsHidden=hidden.clone();child.siblings=siblings.clone();empty.parentsHidden=new String[0];
  script("");
  for(boolean hide:new boolean[]{true,false}){
   String source=add("TD_CHILD","TD_PARENT",hide);
   for(int repeat=0;repeat<3;repeat++){
    script(source+source);
    links(child,hide?new String[]{"TD_KEEP","TD_OTHER","TD_KEEP"}:new String[]{"TD_KEEP","TD_PARENT","TD_OTHER","TD_KEEP"},
     hide?new String[]{"TD_OTHER","TD_PARENT","TD_KEEP"}:new String[]{"TD_OTHER","TD_KEEP"},"idempotent visibility="+hide);
   }
   restored();
  }
  script(add("TD_EMPTY","TD_PARENT",true)+add("TD_EMPTY","TD_PARENT",false));
  links(empty,new String[]{"TD_PARENT"},new String[0],"hidden to visible wins");restored();
  script(add("TD_EMPTY","TD_PARENT",false)+add("TD_EMPTY","TD_PARENT",true));
  links(empty,new String[0],new String[]{"TD_PARENT"},"visible to hidden wins");restored();
  script(remove("TD_CHILD","TD_PARENT"));
  links(child,new String[]{"TD_KEEP","TD_OTHER","TD_KEEP"},new String[]{"TD_OTHER","TD_KEEP"},"remove every copy of one prerequisite");restored();
  script(remove("TD_CHILD","ABSENT")+remove("TD_EMPTY","TD_PARENT"));
  links(child,normal,hidden,"absent link is a no-op");links(empty,null,new String[0],"no-op preserves null and empty");restored();
  script(add("TD_CHILD","TD_PARENT",true)+remove("TD_CHILD","TD_PARENT")+add("TD_CHILD","TD_PARENT",false));
  links(child,new String[]{"TD_KEEP","TD_OTHER","TD_KEEP","TD_PARENT"},new String[]{"TD_OTHER","TD_KEEP"},"mixed actions last call wins");restored();
  String move="mods.thaumcraft.Research.moveResearch(\"TD_CHILD\", \"TD_PREREQS_OTHER\", 4, 4);\n";
  script(move+add("TD_CHILD","TD_PARENT",true)+remove("TD_CHILD","TD_OTHER"));
  links(child,new String[]{"TD_KEEP","TD_KEEP"},new String[]{"TD_PARENT","TD_KEEP"},"editing after tab move");restored();
  script(add("TD_CHILD","TD_PARENT",true)+move+remove("TD_CHILD","TD_OTHER"));restored();
  check(child.category.equals("TD_PREREQS"),"moves undo to original tab");
  script(remove("TD_CHILD","TD_PARENT")+"mods.thaumcraft.Research.removeResearch(\"TD_CHILD\");\n");
  check(ResearchCategories.getResearch("TD_CHILD")==null,"later deletion");restored();
  script(remove("TD_CHILD","TD_PARENT")+"mods.thaumcraft.Research.clearPrereqs(\"TD_CHILD\");\n"+add("TD_CHILD","TD_OTHER",true));
  links(child,new String[0],new String[]{"TD_OTHER"},"clearPrereqs composes with targeted edits");restored();
  script(add("MISSING","TD_PARENT",true)+remove("MISSING","TD_PARENT")+add("TD_CHILD","",false)+remove("TD_CHILD",""));
  links(child,normal,hidden,"invalid operations leave valid research alone");restored();
  script(add("TD_EMPTY","NOT_REGISTERED_YET",true)+remove("TD_EMPTY","NOT_REGISTERED_YET"));
  links(empty,null,new String[0],"dangling prerequisite can be removed");restored();
  AddPrereq action=new AddPrereq("TD_CHILD","TD_PARENT",true);action.apply();
  check(action.canUndo(),"addPrereq remains undoable");
  ResearchCategories.getResearchList(child.category).research.remove(child.key);
  action.undo();check(action.canUndo(),"missing child during undo handled safely");
  ResearchCategories.getResearchList(child.category).research.put(child.key,child);
  child.parents=normal.clone();child.parentsHidden=hidden.clone();
  script("");
 }
}
