package tdtest;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import minetweaker.MineTweakerImplementationAPI;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;
import org.fentanylsolutions.thaumicdabblery.feature.researcheditor.*;
import thaumcraft.api.aspects.*;
import thaumcraft.api.research.*;

public final class ResearchEditorChecks {
 public int checks;
 public static final String TAB="TD_EDITOR",OTHER="TD_EDITOR_OTHER";
 public final Map<String,ResearchItem> items=new LinkedHashMap<>();
 public static final Path FILE=Paths.get("scripts",ResearchEditor.FILE_NAME);
 public void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
 public void script(String text)throws Exception {Files.createDirectories(FILE.getParent());Files.write(FILE,text.getBytes(StandardCharsets.UTF_8));MineTweakerImplementationAPI.reload();}
 public String saved()throws Exception{return new String(Files.readAllBytes(FILE),StandardCharsets.UTF_8);}
 public void setup()throws Exception {
  script("");
  ResourceLocation icon=new ResourceLocation("thaumcraft","textures/aspects/ordo.png");
  ResourceLocation back=new ResourceLocation("thaumcraft","textures/gui/gui_researchback.png");
  ResearchCategories.registerCategory(TAB,icon,back);ResearchCategories.registerCategory(OTHER,icon,back);
  ResearchCategories.registerCategory("TD_EDITOR_EMPTY",icon,back);
  make("TD_A",TAB,0,0);make("TD_B",TAB,2,0);make("TD_C",TAB,0,2);make("TD_D",OTHER,0,0);
  items.get("TD_C").setParents("TD_A","TD_A").setParentsHidden("TD_B").setSiblings("TD_A","TD_A","TD_B");
  items.get("TD_D").setLost().setHidden().setRound().setSpecial().setSecondary();
  script("");
 }
 private void make(String key,String tab,int x,int y){ResearchItem r=new ResearchItem(key,tab,new AspectList().add(Aspect.ORDER,1),x,y,1,new ResourceLocation("thaumcraft","textures/aspects/ordo.png"));r.setPages(new ResearchPage("Editor test"));r.registerResearchItem();items.put(key,r);}
 public void run()throws Exception {
  check(Boolean.FALSE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment")),"production runtime");
  setup();
  overriddenFlags();
  check(ResearchEditor.problem()==null,"ready after reload");
  ResearchPage[] pages=items.get("TD_A").getPages();
  for(int i=1;i<=8;i++){final int x=i;ResearchEditor.edit("move",l->l.move("TD_A",TAB,x,4));}
  String generated=saved();
  check(generated.split("ResearchEditor.move",-1).length==2,"many gestures produce one move");
  check(generated.contains(", 8, 4)"),"last coordinates saved");
  MineTweakerImplementationAPI.reload();
  check(items.get("TD_A").displayColumn==8&&items.get("TD_A").displayRow==4,"generated file replays");
  check(ResearchEditor.undoName()==null,"reload clears history");
  ResearchEditor.edit("restore",l->l.move("TD_A",TAB,0,0));
  check(!saved().contains("ResearchEditor.move"),"original position compacts away across reload");
  ResearchEditor.undo();check(items.get("TD_A").displayColumn==8,"undo position after reload");
  ResearchEditor.redo();check(items.get("TD_A").displayColumn==0,"redo position");
  check(items.get("TD_A").getPages()==pages,"pages retain identity");
  script("");
  ResearchEditor.edit("swap",l->l.swap("TD_A","TD_B"));
  check(items.get("TD_A").displayColumn==2&&items.get("TD_B").displayColumn==0,"occupied positions swap atomically");
  MineTweakerImplementationAPI.reload();check(items.get("TD_A").displayColumn==2&&items.get("TD_B").displayColumn==0,"swap survives script reload");
  script("");ResearchEditor.edit("cross tab swap",l->l.swap("TD_A","TD_D"));
  check(items.get("TD_A").category.equals(OTHER)&&items.get("TD_D").category.equals(TAB),"cross tab swap");
  check(items.get("TD_D").isLost()&&!items.get("TD_A").isLost(),"swap retains properties");
  ResearchEditor.undo();check(items.get("TD_A").category.equals(TAB)&&items.get("TD_D").category.equals(OTHER),"one undo restores both sides");
  ResearchEditor.edit("move to occupied tab",l->l.moveToTab("TD_A",OTHER));
  ResearchLayout.Entry a=ResearchEditor.layout().require("TD_A");
  check(a.tab.equals(OTHER)&&(a.x!=0||a.y!=0)&&Math.abs(a.x)<=1&&Math.abs(a.y)<=1,"move tab chooses nearest free ring");
  ResearchEditor.undo();check(ResearchEditor.redoName()!=null,"redo available");
  ResearchEditor.edit("branch",l->l.require("TD_A").flags|=1);
  check(ResearchEditor.redoName()==null,"new action clears redo");
  script("");
  for(int i=0;i<5;i++){
   final int flag=i;
   ResearchEditor.edit("enable flag",l->l.require("TD_A").flags|=1<<flag);
   check(ResearchEditor.layout().require("TD_A").hasFlag(i),"flag enabled "+i);
   MineTweakerImplementationAPI.reload();check(ResearchEditor.layout().require("TD_A").hasFlag(i),"flag persisted "+i);
   ResearchEditor.edit("restore flag",l->l.require("TD_A").flags&=~(1<<flag));
   check(!saved().contains("ResearchEditor.flag"),"reverted flag removed "+i);
   ResearchEditor.edit("disable original flag",l->l.require("TD_D").flags&=~(1<<flag));
   check(!ResearchEditor.layout().require("TD_D").hasFlag(i),"original true flag can be cleared "+i);
   ResearchEditor.undo();check(ResearchEditor.layout().require("TD_D").hasFlag(i),"exact true flag restored "+i);
  }
  script("");
  ResearchEditor.edit("add cross tab parent",l->l.parent("TD_B",null,"TD_D",false));
  check(Arrays.equals(items.get("TD_B").parents,new String[]{"TD_D"}),"cross tab parent");
  ResearchEditor.edit("hidden parent",l->l.parent("TD_B","TD_D","TD_D",true));
  check(items.get("TD_B").parents.length==0&&Arrays.equals(items.get("TD_B").parentsHidden,new String[]{"TD_D"}),"hidden link toggle");
  MineTweakerImplementationAPI.reload();check(Arrays.equals(items.get("TD_B").parentsHidden,new String[]{"TD_D"}),"hidden parents replay");
  expectInvalid("cycle",()->ResearchEditor.edit("bad",l->l.parent("TD_D",null,"TD_C",false)));
  expectInvalid("self",()->ResearchEditor.edit("bad",l->l.parent("TD_A",null,"TD_A",false)));
  expectInvalid("duplicate",()->ResearchEditor.edit("bad",l->l.parent("TD_C",null,"TD_A",false)));
  expectInvalid("missing",()->ResearchEditor.edit("bad",l->l.parent("TD_A",null,"ABSENT",false)));
  expectInvalid("collision",()->ResearchEditor.edit("bad",l->l.move("TD_A",TAB,2,0)));
  expectInvalid("coordinate overflow",()->ResearchEditor.edit("bad",l->l.move("TD_A",TAB,Integer.MIN_VALUE,0)));
  script("");
  ResearchEditor.edit("replace parent",l->l.parent("TD_C","TD_A","TD_D",false));
  check(Arrays.equals(items.get("TD_C").parents,new String[]{"TD_D"})&&Arrays.equals(items.get("TD_C").parentsHidden,new String[]{"TD_B"}),"replace preserves unrelated hidden parents");
  ResearchEditor.undo();check(Arrays.equals(items.get("TD_C").parents,new String[]{"TD_A","TD_A"}),"undo restores original duplicates");
  ResearchEditor.edit("delete",l->l.delete("TD_A"));
  check(ResearchCategories.getResearch("TD_A")==null,"deletion invalidates lookup cache");
  check(items.get("TD_C").parents.length==0&&Arrays.equals(items.get("TD_C").siblings,new String[]{"TD_B"}),"all incoming ties removed");
  check(!saved().contains("ResearchEditor.parents"),"deletion cleanup not redundantly serialized");
  MineTweakerImplementationAPI.reload();check(ResearchCategories.getResearch("TD_A")==null&&items.get("TD_C").parents.length==0,"deletion replays");
  script("");check(ResearchCategories.getResearch("TD_A")==items.get("TD_A")&&items.get("TD_C").parents.length==2&&items.get("TD_C").siblings.length==3,"rollback restores exact research and ties");
  ResearchEditor.edit("move then delete",l->l.move("TD_A",TAB,5,5));ResearchEditor.edit("delete",l->l.delete("TD_A"));
  check(!saved().contains("ResearchEditor.move"),"deletion supersedes moves");
  ResearchEditor.undo();check(items.get("TD_A").displayColumn==5,"undo deletion restores moved state");
  ResearchEditor.undo();check(items.get("TD_A").displayColumn==0&&items.get("TD_A").parents==null,"undo all restores null arrays");
  script("");
  // The managed file sorts before ordinary scripts: overlays must still run last.
  Path late=Paths.get("scripts/zzzz-editor-baseline.zs");
  Files.write(late,("mods.thaumcraft.Research.moveResearch(\"TD_A\", \""+TAB+"\", 9, 9);\n").getBytes(StandardCharsets.UTF_8));
  script("mods.thaumicdabblery.ResearchEditor.move(\"TD_A\", \""+TAB+"\", 7, 7);\n");
  check(items.get("TD_A").displayColumn==7,"overlay applies after later ordinary script");
  ResearchEditor.edit("return to scripted baseline",l->l.move("TD_A",TAB,9,9));check(!saved().contains("ResearchEditor.move"),"baseline includes ordinary scripts");
  script("");check(items.get("TD_A").displayColumn==9,"ordinary scripted baseline survives reload");
  Files.delete(late);script("");check(items.get("TD_A").displayColumn==0,"overlay undo precedes ordinary undo");
  script("mods.thaumicdabblery.ResearchEditor.move(\"TD_A\", \""+TAB+"\", 2, 0);\n");
  check(ResearchEditor.problem()!=null&&items.get("TD_A").displayColumn==0,"invalid batch rejected atomically");
  script("");
  Files.write(FILE,"// external edit\n".getBytes(StandardCharsets.UTF_8));
  try{ResearchEditor.edit("must fail",l->l.move("TD_A",TAB,4,4));throw new AssertionError("external edit overwritten");}catch(java.io.IOException expected){checks++;}
  check(saved().equals("// external edit\n")&&items.get("TD_A").displayColumn==0&&ResearchEditor.undoName()==null,"save conflict keeps file and model unchanged");
  script("");
  Path temp=Files.createTempDirectory("td-editor-file");EditorFile file=new EditorFile(temp.resolve("test.zs"));file.write("first");file.write("second");
  check(new String(Files.readAllBytes(temp.resolve("test.zs")),StandardCharsets.UTF_8).equals("second"),"atomic writer replaces file");
  Files.delete(temp.resolve("test.zs"));try{file.write("third");throw new AssertionError("external deletion ignored");}catch(java.io.IOException expected){checks++;}
  try(java.util.stream.Stream<Path> stream=Files.list(temp)){check(stream.count()==0,"no temporary files remain");}Files.delete(temp);
  check(ResearchEditor.problem()==null,"final clean baseline");
 }
 private void overriddenFlags()throws Exception {
  // Gadomancy's isHidden() reads the client player, which does not exist during world startup.
  ResearchItem dynamic=new ResearchItem("TD_DYNAMIC",TAB,new AspectList(),20,20,1,new ResourceLocation("thaumcraft","textures/aspects/ordo.png")){
   @Override public boolean isLost(){throw new AssertionError("dynamic isLost called");}
   @Override public boolean isHidden(){throw new AssertionError("dynamic isHidden called");}
   @Override public boolean isSecondary(){throw new AssertionError("dynamic isSecondary called");}
   @Override public boolean isRound(){throw new AssertionError("dynamic isRound called");}
   @Override public boolean isSpecial(){throw new AssertionError("dynamic isSpecial called");}
  };
  dynamic.setHidden().setRound().registerResearchItem();
  script("");
  check(ResearchEditor.layout().require("TD_DYNAMIC").flags==10,"capture stored flags without invoking addon getters");
  ResearchEditor.edit("dynamic flags",l->l.require("TD_DYNAMIC").flags=21);
  check(ResearchLayout.capture().require("TD_DYNAMIC").flags==21,"apply stored flags without invoking addon getters");
  MineTweakerImplementationAPI.reload();
  check(ResearchEditor.layout().require("TD_DYNAMIC").flags==21,"dynamic research flag script replays");
  script("");
  check(ResearchLayout.capture().require("TD_DYNAMIC").flags==10,"rollback restores stored flags without invoking addon getters");
 }
 public interface Checked{void run()throws Exception;}
 private void expectInvalid(String label,Checked action)throws Exception{String before=saved();try{action.run();throw new AssertionError("accepted "+label);}catch(IllegalArgumentException expected){checks++;}check(saved().equals(before),"invalid "+label+" leaves file unchanged");}
}
