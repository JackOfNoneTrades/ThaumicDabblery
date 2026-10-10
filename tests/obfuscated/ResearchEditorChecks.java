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
  items.get("TD_D").setLost().setHidden().setRound().setSpecial().setSecondary().setAutoUnlock();
  make("TD_V",OTHER,4,4);items.get("TD_V").setVirtual();
  script("");
 }
 private void make(String key,String tab,int x,int y){ResearchItem r=new ResearchItem(key,tab,new AspectList().add(Aspect.ORDER,1),x,y,1,new ResourceLocation("thaumcraft","textures/aspects/ordo.png"));r.setPages(new ResearchPage("Editor test"));r.registerResearchItem();items.put(key,r);}
 public void run()throws Exception {
  check(Boolean.FALSE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment")),"production runtime");
  setup();
  collectedScripts();
  exportedBatches();
  removedTabs();
  autoUnlock();
  overriddenFlags();
  warp();
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
  for(int edge:new int[]{-10000,10000}){
   ResearchLayout boundary=ResearchEditor.layout();
   boundary.require("TD_A").x=boundary.require("TD_D").x=edge;
   boundary.require("TD_A").y=boundary.require("TD_D").y=-10000;
   boundary.require("TD_A").tab=TAB;
   boundary.moveToTab("TD_A",OTHER);
   ResearchLayout.Entry placed=boundary.require("TD_A");
   check(placed.tab.equals(OTHER)&&Math.abs(placed.x)<=10000&&Math.abs(placed.y)<=10000&&(placed.x!=edge||placed.y!=-10000),"occupied position at coordinate boundary finds valid free cell");
  }
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
  ResearchEditor.edit("virtual placement",l->{l.toggleFlag("TD_A",5);l.move("TD_A",TAB,2,0);});
  check(items.get("TD_A").isVirtual()&&items.get("TD_A").displayColumn==2,"pending virtual flag permits overlap before applying");
  MineTweakerImplementationAPI.reload();
  check(items.get("TD_A").isVirtual()&&items.get("TD_A").displayColumn==2,"virtual overlap survives generated move-before-flag script");
  ResearchEditor.edit("make visible",l->l.toggleFlag("TD_A",5));
  check(!items.get("TD_A").isVirtual()&&(items.get("TD_A").displayColumn!=2||items.get("TD_A").displayRow!=0),"clearing virtual finds a free cell");
  ResearchEditor.undo();check(items.get("TD_A").isVirtual()&&items.get("TD_A").displayColumn==2&&items.get("TD_A").displayRow==0,"undo restores virtual flag and overlapping position together");
  ResearchEditor.redo();check(!items.get("TD_A").isVirtual(),"redo clears virtual");
  MineTweakerImplementationAPI.reload();check(!items.get("TD_A").isVirtual()&&items.get("TD_A").displayRow!=0,"devirtualized placement survives reload");
  script("");
  ResearchEditor.edit("virtual destination occupant",l->{l.toggleFlag("TD_B",5);l.move("TD_A",TAB,2,0);});
  check(items.get("TD_B").isVirtual()&&items.get("TD_A").displayColumn==2,"collision check sees pending virtual flag on occupant");
  script("");
  ResearchEditor.edit("clear original virtual",l->l.toggleFlag("TD_V",5));
  check(!items.get("TD_V").isVirtual()&&saved().contains("\"Virtual\", false"),"original virtual flag can be cleared and saved");
  MineTweakerImplementationAPI.reload();check(!items.get("TD_V").isVirtual(),"original virtual cleared after reload");
  ResearchEditor.edit("restore original virtual",l->l.toggleFlag("TD_V",5));
  check(items.get("TD_V").isVirtual()&&!saved().contains("ResearchEditor.flag"),"restoring virtual baseline compacts script");
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
 private void warp()throws Exception {
  Map<Object,Integer> map=cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(thaumcraft.api.ThaumcraftApi.class,null,"warpMap");
  check(!map.containsKey("TD_A"),"fixture starts without research warp");
  for(int amount:new int[]{1,3,8})ResearchEditor.edit("warp",l->l.setWarp("TD_A",amount));
  check(thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==8&&saved().split("ResearchEditor.warp",-1).length==2,"repeated warp edits compact to final value");
  MineTweakerImplementationAPI.reload();check(thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==8,"warp script replay");
  ResearchEditor.edit("clear warp",l->l.setWarp("TD_A",0));
  check(!map.containsKey("TD_A")&&!saved().contains("ResearchEditor.warp"),"zero clears warp and original-state edit compacts away");
  ResearchEditor.undo();check(thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==8,"warp undo");
  ResearchEditor.redo();check(!map.containsKey("TD_A"),"warp redo");
  expectInvalid("negative warp",()->ResearchEditor.edit("bad",l->l.setWarp("TD_A",-1)));
  script("");
  Path late=Paths.get("scripts/zzzz-warp-baseline.zs");
  Files.write(late,"mods.thaumcraft.Warp.addToResearch(\"TD_A\", 4);\n".getBytes(StandardCharsets.UTF_8));
  script("mods.thaumicdabblery.ResearchEditor.warp(\"TD_A\", 9);\n");
  check(thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==9,"editor warp applies after ordinary scripts");
  ResearchEditor.edit("restore scripted warp",l->l.setWarp("TD_A",4));
  check(!saved().contains("ResearchEditor.warp"),"scripted warp is editor baseline");
  script("mods.thaumicdabblery.ResearchEditor.warp(\"TD_A\", 0);\n");
  check(thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==0,"zero overrides existing scripted warp");
  script("");check(thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==4,"removing editor override restores scripted warp");
  Files.delete(late);script("");check(thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==0,"ordinary warp undo runs after editor undo");
  ResearchEditor.edit("warp before delete",l->l.setWarp("TD_A",7));ResearchEditor.edit("delete",l->l.delete("TD_A"));
  check(!map.containsKey("TD_A")&&!saved().contains("ResearchEditor.warp"),"deletion supersedes warp edit");
  ResearchEditor.undo();check(thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==7,"deletion undo restores warp");
  script("");
  script("mods.thaumicdabblery.ResearchEditor.warp(\"TD_A\", -1);\n");
  check(ResearchEditor.problem()!=null&&thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==0,"negative scripted warp rejected atomically");
  script("");
 }
 private void removedTabs()throws Exception {
  ResourceLocation icon=new ResourceLocation("thaumcraft","textures/aspects/ordo.png");
  ResearchCategories.registerCategory("TD_TAB_SOURCE",icon,icon);ResearchCategories.registerCategory("TD_TAB_DEST",icon,icon);
  make("TD_RA","TD_TAB_SOURCE",0,0);make("TD_RB","TD_TAB_SOURCE",2,0);make("TD_RD","TD_TAB_DEST",0,0);make("TD_RL","TD_TAB_DEST",2,0);
  items.get("TD_RB").setParents("TD_RA");
  items.get("TD_RL").setParents("TD_RA","TD_RA","TD_RB").setParentsHidden("TD_RB").setSiblings("TD_RA","TD_RB","TD_RB");
  script("");
  ResearchCategoryList originalTab=ResearchCategories.getResearchList("TD_TAB_SOURCE");
  List<String> tabOrder=new ArrayList<>(ResearchCategories.researchCategories.keySet());ResearchPage[] pages=items.get("TD_RA").getPages();
  String e="mods.thaumicdabblery.ResearchEditor.",m="mods.thaumcraft.Research.";
  String remove=m+"removeTab(\"TD_TAB_SOURCE\");\n",move=e+"move(\"TD_RA\", \"TD_TAB_DEST\", 4, 4);\n";
  String flag=e+"flag(\"TD_RA\", \"Hidden\", true);\n",other=e+"warp(\"TD_RD\", 3);\n";
  Path regular=Paths.get("scripts/zzz-tab-removal.zs");
  try {
   for(boolean exported:new boolean[]{false,true}){
    String batch=remove+flag+move+other;
    if(exported){Files.write(regular,batch.getBytes(StandardCharsets.UTF_8));script("");}else script(batch);
    check(ResearchEditor.problem()==null&&ResearchCategories.getResearchList("TD_TAB_SOURCE")==null,"deferred tab removal works in "+(exported?"regular":"managed")+" script, regardless of call placement");
    check(ResearchCategories.getResearch("TD_RA")==items.get("TD_RA")&&items.get("TD_RA").category.equals("TD_TAB_DEST")&&items.get("TD_RA").isHidden()&&items.get("TD_RA").getPages()==pages,"moved research survives with properties and page identity");
    check(ResearchCategories.getResearch("TD_RB")==null&&thaumcraft.api.ThaumcraftApi.getWarp("TD_RD")==3,"remaining research removed without discarding unrelated edits");
    check(Arrays.equals(items.get("TD_RL").parents,new String[]{"TD_RA","TD_RA"})&&items.get("TD_RL").parentsHidden.length==0&&Arrays.equals(items.get("TD_RL").siblings,new String[]{"TD_RA"}),"tab removal cuts all deleted links but retains links to moved-out research");
    ResearchEditor.edit("new edit after tab removal",l->l.setWarp("TD_RD",5));String compact=saved();
    check(!compact.contains("ResearchEditor.remove(\"TD_RB\")")&&!compact.contains("ResearchEditor.parents")&&compact.contains(".removeTab")==!exported,"saving retains only owned tab deletion and no incidental entry/link cleanup");
    ResearchEditor.undo();check(thaumcraft.api.ThaumcraftApi.getWarp("TD_RD")==3&&ResearchCategories.getResearchList("TD_TAB_SOURCE")==null,"UI undo preserves removed tab");
    ResearchEditor.redo();MineTweakerImplementationAPI.reload();
    check(ResearchEditor.problem()==null&&ResearchCategories.getResearch("TD_RA")!=null&&ResearchCategories.getResearch("TD_RB")==null&&thaumcraft.api.ThaumcraftApi.getWarp("TD_RD")==5,"saved tab removal survives reload");
    Files.deleteIfExists(regular);script("");
    check(ResearchCategories.getResearchList("TD_TAB_SOURCE")==originalTab&&new ArrayList<>(ResearchCategories.researchCategories.keySet()).equals(tabOrder)&&items.get("TD_RA").category.equals("TD_TAB_SOURCE")&&!items.get("TD_RA").isHidden(),"removing script restores category identity, order and moved entry");
    check(Arrays.equals(items.get("TD_RL").parents,new String[]{"TD_RA","TD_RA","TD_RB"})&&Arrays.equals(items.get("TD_RL").parentsHidden,new String[]{"TD_RB"})&&Arrays.equals(items.get("TD_RL").siblings,new String[]{"TD_RA","TD_RB","TD_RB"}),"reload undo restores exact duplicate parent and sibling links");
   }
   Files.write(regular,remove.getBytes(StandardCharsets.UTF_8));script(move+flag);
   check(ResearchEditor.problem()==null&&ResearchCategories.getResearch("TD_RA")!=null,"regular tab removal waits for managed moves");
   ResearchEditor.edit("compact rescued entry",l->l.setWarp("TD_RD",2));
   check(saved().contains("ResearchEditor.move(\"TD_RA\"")&&!saved().contains("removeTab")&&!saved().contains("ResearchEditor.remove("),"managed rescue remains while regular removal stays out of saved file");
   MineTweakerImplementationAPI.reload();check(ResearchEditor.problem()==null&&ResearchCategories.getResearch("TD_RA")!=null,"managed rescue replays after save");
   Files.delete(regular);script("");
   script(move+remove);String export=saved();Files.write(regular,export.getBytes(StandardCharsets.UTF_8));script("");
   ResearchEditor.edit("next exported removal batch",l->l.setWarp("TD_RD",6));
   check(!saved().contains("move(")&&!saved().contains("removeTab")&&ResearchCategories.getResearch("TD_RA")!=null,"exporting removal and move makes both baseline");
   Files.delete(regular);script("");
   script(e+"move(\"TD_RA\", \"TD_TAB_DEST\", 0, 0);\n"+e+"move(\"TD_RD\", \"TD_TAB_SOURCE\", 0, 0);\n"+remove);
   check(ResearchEditor.problem()==null&&ResearchCategories.getResearch("TD_RA")!=null&&ResearchCategories.getResearch("TD_RD")==null,"cross-tab occupied swap remains atomic before deletion");
   ResearchEditor.edit("save swapped removal",l->l.setWarp("TD_RA",2));MineTweakerImplementationAPI.reload();
   check(ResearchEditor.problem()==null&&ResearchCategories.getResearch("TD_RD")==null,"saving research moved into removed tab preserves its deletion");script("");
   script(remove+e+"removeTab(\"TD_TAB_DEST\");\n"+remove+e+"removeTab(\"TD_EDITOR_EMPTY\");\n");
   check(ResearchEditor.problem()==null&&ResearchCategories.getResearchList("TD_TAB_DEST")==null&&ResearchCategories.getResearchList("TD_EDITOR_EMPTY")==null,"multiple, duplicate and empty tab removals");script("");
   script(other+e+"removeTab(\"TYPO_TAB\");\n");check(ResearchEditor.problem()==null&&thaumcraft.api.ThaumcraftApi.getWarp("TD_RD")==3,"removing a missing tab is harmless");script("");
   Files.write(regular,remove.getBytes(StandardCharsets.UTF_8));script(e+"flag(\"TYPO_KEY\", \"Hidden\", true);\n");
   check(ResearchEditor.problem()!=null&&ResearchCategories.getResearchList("TD_TAB_SOURCE")==null,"invalid managed batch preserves valid baseline removal");Files.delete(regular);script("");
   for(String ordinary:new String[]{m+"removeResearch(\"TD_RA\");\n"}){
    for(boolean exported:new boolean[]{false,true}){
     String stale=flag+move+e+"warp(\"TD_RA\", 9);\n"+e+"parents(\"TD_RA\", [], []);\n"+e+"remove(\"TD_RA\");\n"+other;
     Files.write(regular,((exported?stale:"")+ordinary).getBytes(StandardCharsets.UTF_8));script(exported?"":stale);
     check(ResearchEditor.problem()==null&&ResearchCategories.getResearch("TD_RA")==null&&thaumcraft.api.ThaumcraftApi.getWarp("TD_RD")==3,"confirmed ordinary deletion skips stale declarations in "+(exported?"regular":"managed")+" layer");
     ResearchEditor.edit("save after ordinary deletion",l->l.setWarp("TD_RD",4));
     check(!saved().contains("TD_RA"),"obsolete declarations disappear on next editor save");
     Files.delete(regular);script("");check(ResearchCategories.getResearch("TD_RA")==items.get("TD_RA"),"ordinary removal remains undoable");
    }
   }
   Files.write(regular,(m+"removeResearch(\"TD_RA\");\n").getBytes(StandardCharsets.UTF_8));
   script(e+"parents(\"TD_RD\", [\"TD_RA\", \"TD_RB\", \"TD_RA\"], [\"TD_RA\"]);\n");
   check(ResearchEditor.problem()==null&&Arrays.equals(items.get("TD_RD").parents,new String[]{"TD_RB"})&&items.get("TD_RD").parentsHidden.length==0,"ordinary deleted prerequisites are filtered without discarding live parents");
   script(e+"parents(\"TD_RD\", [\"TD_RA\", \"TYPO_KEY\"], []);\n");check(ResearchEditor.problem()!=null,"unknown prerequisite still rejects batch");
   Files.delete(regular);script("");
   script(flag+e+"flag(\"TYPO_KEY\", \"Hidden\", true);\n");check(ResearchEditor.problem()!=null&&!items.get("TD_RA").isHidden(),"unknown research still rejects whole batch");script("");
   Files.write(regular,(m+"removeResearch(\"TD_RA\");\n"+m+"addResearch(\"TD_RA\", \"TD_TAB_DEST\", \"\", 6, 6, 0, <minecraft:cookie>);\n").getBytes(StandardCharsets.UTF_8));script(flag);
   check(ResearchEditor.problem()==null&&ResearchCategories.getResearch("TD_RA")!=items.get("TD_RA")&&ResearchCategories.getResearch("TD_RA").isHidden(),"research recreated with same key still receives editor changes");Files.delete(regular);script("");
   check(ResearchCategories.getResearch("TD_RA")==items.get("TD_RA")&&!items.get("TD_RA").isHidden(),"replacement research undo restores original identity and flags");
   Files.write(regular,(m+"addResearch(\"TD_REMOVED_TEMP\", \"TD_TAB_DEST\", \"\", 6, 6, 0, <minecraft:cookie>);\n"+m+"removeResearch(\"TD_REMOVED_TEMP\");\n").getBytes(StandardCharsets.UTF_8));
   String temporary=e+"flag(\"TD_REMOVED_TEMP\", \"Hidden\", true);\n";script(temporary);
   check(ResearchEditor.problem()==null,"same-reload creation and ordinary deletion is recognized");Files.delete(regular);script(temporary);
   check(ResearchEditor.problem()!=null,"removal tracking does not leak into later reloads");script("");
   Files.write(regular,(m+"removeTab(\"TD_TAB_SOURCE\");\n").getBytes(StandardCharsets.UTF_8));script(remove+other);
   check(ResearchEditor.problem()==null&&thaumcraft.api.ThaumcraftApi.getWarp("TD_RD")==3,"deferred removal of a tab already removed by ordinary scripts is harmless");
   Files.delete(regular);script("");
   script(e+"removeTab(\"TD_TAB_SOURCE\");\n"+move+flag);
   check(ResearchEditor.problem()==null&&ResearchCategories.getResearch("TD_RA")==items.get("TD_RA")&&ResearchCategories.getResearchList("TD_TAB_SOURCE")==null,"legacy editor removal is an alias");
   ResearchEditor.edit("save alias",l->l.setWarp("TD_RD",3));check(saved().contains("mods.thaumcraft.Research.removeTab")&&!saved().contains("ResearchEditor.removeTab"),"saved scripts use ordinary removal");script("");
   String add=m+"addTab(\"TD_TAB_SOURCE\", \"minecraft\", \"textures/items/diamond.png\");\n";
   String fresh=m+"addResearch(\"TD_FRESH\", \"TD_TAB_SOURCE\", \"\", 0, 0, 0, <minecraft:cookie>);\n";
   Files.write(regular,(remove+add+fresh).getBytes(StandardCharsets.UTF_8));script(move+flag);
   check(ResearchEditor.problem()==null,"replacement allows editor rescue and overlapping fresh positions");
   ResearchCategoryList replacement=ResearchCategories.getResearchList("TD_TAB_SOURCE");
   check(replacement!=null&&replacement!=originalTab&&replacement.icon.equals(new ResourceLocation("minecraft","textures/items/diamond.png")),"new tab identity and icon survive old removal");
   check(ResearchCategories.getResearch("TD_RA")==items.get("TD_RA")&&items.get("TD_RA").category.equals("TD_TAB_DEST")&&ResearchCategories.getResearch("TD_RB")==null&&ResearchCategories.getResearch("TD_FRESH")!=null,"old entry rescued, old remainder removed, new entry retained");
   check(Arrays.equals(items.get("TD_RL").parents,new String[]{"TD_RA","TD_RA"}),"replacement retains rescued links and cuts deleted links");
   ResearchEditor.edit("save replacement",l->l.setWarp("TD_RD",2));check(saved().contains("move(\"TD_RA\"")&&!saved().contains("TD_RB")&&!saved().contains("addTab")&&!saved().contains("removeTab"),"saving retains rescue without copying replacement or incidental deletion");
   for(int i=0;i<3;i++){MineTweakerImplementationAPI.reload();check(ResearchEditor.problem()==null&&ResearchCategories.getResearch("TD_RA")==items.get("TD_RA")&&ResearchCategories.getResearch("TD_RB")==null&&ResearchCategories.getResearch("TD_FRESH")!=null,"replacement reload remains stable");}
   Files.delete(regular);script("");check(ResearchCategories.getResearchList("TD_TAB_SOURCE")==originalTab&&ResearchCategories.getResearch("TD_RA")==items.get("TD_RA")&&ResearchCategories.getResearch("TD_RB")==items.get("TD_RB")&&ResearchCategories.getResearch("TD_FRESH")==null&&new ArrayList<>(ResearchCategories.researchCategories.keySet()).equals(tabOrder),"replacement undo restores original category, entries and order");
   Files.write(regular,(remove+add+fresh).getBytes(StandardCharsets.UTF_8));script(e+"move(\"TD_RB\", \"TD_TAB_SOURCE\", 2, 0);\n");
   check(ResearchEditor.problem()==null&&ResearchCategories.getResearch("TD_RB")==items.get("TD_RB")&&ResearchCategories.getResearch("TD_RA")==null,"explicit same-tab move retains entry in replacement");ResearchEditor.edit("save same-tab rescue",l->l.setWarp("TD_RD",1));check(saved().contains("move(\"TD_RB\""),"same-coordinate rescue remains in saved script");MineTweakerImplementationAPI.reload();check(ResearchCategories.getResearch("TD_RB")==items.get("TD_RB"),"saved same-tab rescue survives reload");
   Files.delete(regular);script("");
   String reuse=m+"addResearch(\"TD_RA\", \"TD_TAB_SOURCE\", \"\", 0, 0, 0, <minecraft:cookie>);\n";
   Files.write(regular,(remove+add+reuse).getBytes(StandardCharsets.UTF_8));script(flag);
   check(ResearchEditor.problem()==null&&ResearchCategories.getResearch("TD_RA")!=items.get("TD_RA")&&ResearchCategories.getResearch("TD_RA").isHidden(),"new research wins reused key and receives editor edits");Files.delete(regular);script("");
   Files.write(regular,(remove+add+reuse+m+"removeResearch(\"TD_RA\");\n").getBytes(StandardCharsets.UTF_8));script(move+e+"move(\"TD_RB\", \"TD_TAB_DEST\", 6, 6);\n");
   check(ResearchEditor.problem()==null&&ResearchCategories.getResearch("TD_RA")==null&&ResearchCategories.getResearch("TD_RB")==items.get("TD_RB")&&items.get("TD_RB").parents.length==0,"explicit deletion does not resurrect retired research or its links");Files.delete(regular);script("");
   Files.write(regular,(m+"removeResearch(\"TD_RA\");\n"+reuse+remove+add+fresh).getBytes(StandardCharsets.UTF_8));script(move);
   check(ResearchEditor.problem()==null&&ResearchCategories.getResearch("TD_RA")!=null&&ResearchCategories.getResearch("TD_RA")!=items.get("TD_RA")&&ResearchCategories.getResearch("TD_RA").category.equals("TD_TAB_DEST"),"earlier removal does not hide later re-created retired key");Files.delete(regular);script("");
   Files.write(regular,(remove+add+fresh+e+"flag(\"TYPO_KEY\", \"Hidden\", true);\n").getBytes(StandardCharsets.UTF_8));script("");
   check(ResearchEditor.problem()!=null&&ResearchCategories.getResearch("TD_RA")==null&&ResearchCategories.getResearch("TD_FRESH")!=null&&items.get("TD_RL").parents.length==0,"invalid baseline still cleans references to discarded retired contents");Files.delete(regular);script("");
   Files.write(regular,(remove+add+fresh).getBytes(StandardCharsets.UTF_8));script(e+"flag(\"TYPO_KEY\", \"Hidden\", true);\n");
   check(ResearchEditor.problem()!=null&&ResearchCategories.getResearch("TD_RA")==null&&ResearchCategories.getResearch("TD_FRESH")!=null&&items.get("TD_RL").parents.length==0,"invalid overlay keeps replacement and cleans discarded references");Files.delete(regular);script("");
   Files.write(regular,(remove+add+reuse+remove+add+fresh).getBytes(StandardCharsets.UTF_8));script(move);
   check(ResearchEditor.problem()==null&&ResearchCategories.getResearch("TD_RA")!=items.get("TD_RA")&&ResearchCategories.getResearch("TD_RA").category.equals("TD_TAB_DEST")&&ResearchCategories.getResearch("TD_FRESH")!=null,"multiple replacements rescue newest matching key");Files.delete(regular);script("");check(ResearchCategories.getResearch("TD_RA")==items.get("TD_RA"),"multiple replacement undo restores oldest identity");
   Files.write(regular,(remove+add+fresh+remove).getBytes(StandardCharsets.UTF_8));script(move);
   check(ResearchEditor.problem()==null&&ResearchCategories.getResearch("TD_RA")==items.get("TD_RA")&&ResearchCategories.getResearchList("TD_TAB_SOURCE")==null&&ResearchCategories.getResearch("TD_FRESH")==null,"final removal deletes replacement but retains rescued entry");Files.delete(regular);script("");
   Files.write(regular,(m+"removeTab(\"TD_TEMP_TAB\");\n"+m+"addTab(\"TD_TEMP_TAB\", \"minecraft\", \"textures/items/diamond.png\");\n").getBytes(StandardCharsets.UTF_8));script("");check(ResearchCategories.getResearchList("TD_TEMP_TAB")!=null,"remove missing then add creates tab");Files.delete(regular);script("");check(ResearchCategories.getResearchList("TD_TEMP_TAB")==null,"undo missing-tab replacement removes new tab");
  } finally {Files.deleteIfExists(regular);script("");}
 }
 private void autoUnlock()throws Exception {
  net.minecraft.server.MinecraftServer server=net.minecraft.server.MinecraftServer.func_71276_C();
  net.minecraft.entity.player.EntityPlayerMP player=net.minecraftforge.common.util.FakePlayerFactory.get(server.func_71218_a(0),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TD_AutoUnlock"));
  player.field_71135_a=new net.minecraft.network.NetHandlerPlayServer(server,new net.minecraft.network.NetworkManager(false){private final io.netty.channel.Channel sink=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());public io.netty.channel.Channel channel(){return sink;}},player){@Override public void func_147359_a(net.minecraft.network.Packet packet){}};
  String name=player.func_70005_c_();
  thaumcraft.common.lib.research.PlayerKnowledge knowledge=thaumcraft.common.Thaumcraft.proxy.getPlayerKnowledge();
  thaumcraft.common.lib.events.EventHandlerEntity events=new thaumcraft.common.lib.events.EventHandlerEntity();
  Path directory=Files.createTempDirectory("td-autounlock-");String uuid=player.func_110124_au().toString();
  net.minecraftforge.event.entity.player.PlayerEvent.LoadFromFile load=new net.minecraftforge.event.entity.player.PlayerEvent.LoadFromFile(player,directory.toFile(),uuid);
  net.minecraftforge.event.entity.player.PlayerEvent.SaveToFile save=new net.minecraftforge.event.entity.player.PlayerEvent.SaveToFile(player,directory.toFile(),uuid);
  boolean wuss=thaumcraft.common.config.Config.wuss;
  try {
   thaumcraft.common.config.Config.wuss=false;
   events.playerLoad(load);int initialWarp=knowledge.getWarpTotal(name);
   ResearchEditor.edit("auto unlock",l->{l.toggleFlag("TD_A",6);l.parent("TD_A",null,"TD_B",false);l.setWarp("TD_A",4);});
   check(items.get("TD_A").isAutoUnlock()&&saved().contains("\"AutoUnlock\", true"),"AutoUnlock checkbox saves native flag");
   check(!thaumcraft.common.lib.research.ResearchManager.isResearchComplete(name,"TD_A")&&knowledge.getWarpTotal(name)==initialWarp,"enabling AutoUnlock does not complete research or award warp");
   ResearchEditor.undo();check(!items.get("TD_A").isAutoUnlock()&&!saved().contains("AutoUnlock"),"AutoUnlock undo compacts flag");
   ResearchEditor.redo();check(items.get("TD_A").isAutoUnlock(),"AutoUnlock redo restores flag");
   MineTweakerImplementationAPI.reload();
   check(items.get("TD_A").isAutoUnlock()&&!thaumcraft.common.lib.research.ResearchManager.isResearchComplete(name,"TD_A")&&knowledge.getWarpTotal(name)==initialWarp,"AutoUnlock script reload changes definition only");
   events.playerLoad(load);
   check(thaumcraft.common.lib.research.ResearchManager.isResearchComplete(name,"TD_A")&&!thaumcraft.common.lib.research.ResearchManager.isResearchComplete(name,"TD_B"),"native player load grants AutoUnlock despite incomplete prerequisite");
   check(knowledge.getWarpTotal(name)==initialWarp+4,"native AutoUnlock completion awards attached warp");
   net.minecraft.nbt.NBTTagCompound nbt=new net.minecraft.nbt.NBTTagCompound();
   thaumcraft.common.lib.research.ResearchManager.saveResearchNBT(nbt,player);
   check(!nbt.toString().contains("TD_A"),"native saves omit automatically unlocked research");
   events.playerSave(save);events.playerLoad(load);
   check(knowledge.getWarpTotal(name)==initialWarp+8,"native reload grants automatic research warp again");
   events.playerSave(save);
   ResearchEditor.edit("disable auto unlock",l->l.toggleFlag("TD_A",6));
   check(!items.get("TD_A").isAutoUnlock()&&thaumcraft.common.lib.research.ResearchManager.isResearchComplete(name,"TD_A")&&knowledge.getWarpTotal(name)==initialWarp+8,"disabling AutoUnlock leaves current completion and warp intact");
   events.playerLoad(load);
   check(!thaumcraft.common.lib.research.ResearchManager.isResearchComplete(name,"TD_A")&&knowledge.getWarpTotal(name)==initialWarp+8,"disabled automatic research is not restored from prior automatic-only save");
   script("");
   ResearchEditor.edit("clear original AutoUnlock",l->l.toggleFlag("TD_D",6));
   check(!items.get("TD_D").isAutoUnlock()&&saved().contains("\"AutoUnlock\", false"),"original AutoUnlock can be cleared in script");
   MineTweakerImplementationAPI.reload();check(!items.get("TD_D").isAutoUnlock(),"false AutoUnlock script survives reload");
   ResearchEditor.edit("restore original AutoUnlock",l->l.toggleFlag("TD_D",6));
   check(items.get("TD_D").isAutoUnlock()&&!saved().contains("AutoUnlock"),"returning to original AutoUnlock compacts override");
  } finally {
   script("");
   thaumcraft.common.config.Config.wuss=wuss;knowledge.wipePlayerKnowledge(name);
   try(java.util.stream.Stream<Path> paths=Files.walk(directory)){for(Path path:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator)Files.delete(path);}
  }
 }
 private void overriddenFlags()throws Exception {
  // Gadomancy's isHidden() reads the client player, which does not exist during world startup.
  ResearchItem dynamic=new ResearchItem("TD_DYNAMIC",TAB,new AspectList(),20,20,1,new ResourceLocation("thaumcraft","textures/aspects/ordo.png")){
   @Override public boolean isLost(){throw new AssertionError("dynamic isLost called");}
   @Override public boolean isHidden(){throw new AssertionError("dynamic isHidden called");}
   @Override public boolean isSecondary(){throw new AssertionError("dynamic isSecondary called");}
   @Override public boolean isRound(){throw new AssertionError("dynamic isRound called");}
   @Override public boolean isSpecial(){throw new AssertionError("dynamic isSpecial called");}
   @Override public boolean isAutoUnlock(){throw new AssertionError("dynamic isAutoUnlock called");}
  };
  dynamic.setHidden().setRound().setAutoUnlock().registerResearchItem();
  script("");
  check(ResearchEditor.layout().require("TD_DYNAMIC").flags==74,"capture stored flags without invoking addon getters");
  ResearchEditor.edit("dynamic flags",l->l.require("TD_DYNAMIC").flags=21);
  check(ResearchLayout.capture().require("TD_DYNAMIC").flags==21,"apply stored flags without invoking addon getters");
  MineTweakerImplementationAPI.reload();
  check(ResearchEditor.layout().require("TD_DYNAMIC").flags==21,"dynamic research flag script replays");
  script("");
  check(ResearchLayout.capture().require("TD_DYNAMIC").flags==74,"rollback restores stored flags without invoking addon getters");
 }
 public interface Checked{void run()throws Exception;}
 private void expectInvalid(String label,Checked action)throws Exception{String before=saved();try{action.run();throw new AssertionError("accepted "+label);}catch(IllegalArgumentException expected){checks++;}check(saved().equals(before),"invalid "+label+" leaves file unchanged");}
 private void collectedScripts()throws Exception {
  for(int mode=0;mode<3;mode++){
   final int kind=mode;final boolean[] closed={false};final byte[] bytes=kind==1?new byte[0]:"// collected script".getBytes(StandardCharsets.UTF_8);
   java.io.InputStream stream=new java.io.InputStream(){int offset;
    public int available(){return bytes.length-offset;}
    public int read()throws java.io.IOException{if(kind==2)throw new java.io.IOException("injected read failure");return offset<bytes.length?bytes[offset++]&255:-1;}
    public void close(){closed[0]=true;}
   };
   byte[] packed=minetweaker.runtime.providers.ScriptProviderMemory.collect(singleStream(stream));
   check(closed[0],"script collection closes "+(kind==0?"successful":kind==1?"empty":"failed")+" input");
   if(kind==0){minetweaker.runtime.IScriptIterator restored=new minetweaker.runtime.providers.ScriptProviderMemory(packed).getScripts().next();check(restored.next(),"collected script remains present");try(java.io.InputStream input=restored.open()){check(Arrays.equals(minetweaker.util.FileUtil.read(input),bytes),"collected script bytes unchanged");}}
  }
  // Retain the actual file stream so GC cannot conceal a leaked Windows file handle.
  try(java.io.FileInputStream input=new java.io.FileInputStream(FILE.toFile())){
   minetweaker.runtime.providers.ScriptProviderMemory.collect(singleStream(input));
   try{input.available();throw new AssertionError("script file handle retained after collection");}catch(java.io.IOException closed){check(true,"actual script file handle released immediately");}
  }
  for(int i=0;i<3;i++){
   script("");
   ResearchEditor.edit("parent after reload",l->l.parent("TD_A",null,"TD_B",false));
   check(Arrays.equals(items.get("TD_A").parents,new String[]{"TD_B"})&&saved().contains("ResearchEditor.parents"),"first parent edit after reload saves");
  }
  script("");
 }
 private minetweaker.runtime.IScriptProvider singleStream(final java.io.InputStream input){
  return ()->Collections.<minetweaker.runtime.IScriptIterator>singletonList(new minetweaker.runtime.IScriptIterator(){boolean first=true;
   public String getGroupName(){return "collected.zs";}public String getName(){return "collected.zs";}
   public boolean next(){boolean result=first;first=false;return result;}
   public java.io.InputStream open(){return input;}
  }).iterator();
 }

 private void exportedBatches()throws Exception {
  Path regular=Paths.get("scripts/zzzz-exported-editor.zs"),second=Paths.get("scripts/aaa-exported-editor.zs");
  try {
   ResearchEditor.edit("first batch",l->{l.swap("TD_A","TD_B");l.toggleFlag("TD_A",1);l.toggleFlag("TD_A",6);l.setWarp("TD_A",7);l.parent("TD_A",null,"TD_D",true);l.delete("TD_V");});
   String exported=saved();Files.write(regular,exported.getBytes(StandardCharsets.UTF_8));script("");
   check(ResearchEditor.problem()==null&&items.get("TD_A").displayColumn==2&&items.get("TD_B").displayColumn==0,"exported occupied swap applies as a batch");
   check(items.get("TD_A").isHidden()&&items.get("TD_A").isAutoUnlock()&&thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==7&&Arrays.equals(items.get("TD_A").parentsHidden,new String[]{"TD_D"})&&ResearchCategories.getResearch("TD_V")==null,"export retains flags, warp, parents and deletion");
   ResearchEditor.edit("next batch",l->l.setWarp("TD_C",3));
   check(saved().contains("ResearchEditor.warp(\"TD_C\", 3)")&&!saved().contains("\"TD_A\"")&&!saved().contains("\"TD_B\"")&&!saved().contains("\"TD_V\""),"exported declarations do not reappear on next save");
   check(new String(Files.readAllBytes(regular),StandardCharsets.UTF_8).equals(exported),"editor leaves regular script untouched");
   ResearchEditor.undo();check(!saved().contains("ResearchEditor.")&&thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==7&&ResearchCategories.getResearch("TD_V")==null,"undo removes only new batch");
   ResearchEditor.redo();check(thaumcraft.api.ThaumcraftApi.getWarp("TD_C")==3&&!saved().contains("\"TD_A\""),"redo excludes exported baseline");
   ResearchEditor.edit("override exported value",l->l.setWarp("TD_A",9));
   check(saved().contains("ResearchEditor.warp(\"TD_A\", 9)")&&!saved().contains("ResearchEditor.move"),"new edits override exported values without duplicating other properties");
   MineTweakerImplementationAPI.reload();check(thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==9,"managed overrides apply after later-named regular scripts");
   ResearchEditor.edit("return to exported value",l->l.setWarp("TD_A",7));check(!saved().contains("\"TD_A\""),"returning to exported baseline removes override");
   Files.write(second,saved().getBytes(StandardCharsets.UTF_8));script("");
   ResearchEditor.edit("third batch",l->l.setWarp("TD_C",4));
   check(saved().contains("ResearchEditor.warp(\"TD_C\", 4)")&&!saved().contains("\"TD_A\""),"multiple exported batches establish new baseline");
   ResearchEditor.undo();check(!saved().contains("ResearchEditor.")&&thaumcraft.api.ThaumcraftApi.getWarp("TD_C")==3,"undo returns to second exported batch");
   Files.delete(second);script("");check(thaumcraft.api.ThaumcraftApi.getWarp("TD_C")==0&&thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==7,"removing one exported script restores only its changes");
   Files.delete(regular);script("");
   check(items.get("TD_A").displayColumn==0&&items.get("TD_B").displayColumn==2&&!items.get("TD_A").isHidden()&&!items.get("TD_A").isAutoUnlock()&&items.get("TD_A").parentsHidden==null&&thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==0&&ResearchCategories.getResearch("TD_V")==items.get("TD_V"),"removing exports restores original layout and deleted entries");
   String helper="import mods.thaumicdabblery.ResearchEditor as E;\nfunction applyBatch() as void { E.warp(\"TD_A\", 5); }\napplyBatch();\n";
   Files.write(regular,helper.getBytes(StandardCharsets.UTF_8));script(helper.replace(", 5)",", 9)"));
   ResearchEditor.edit("helper batch",l->l.setWarp("TD_C",2));
   check(thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==9&&saved().contains("ResearchEditor.warp(\"TD_A\", 9)"),"helper functions and aliases retain managed-file ownership");
   script("");ResearchEditor.edit("helper baseline",l->l.setWarp("TD_C",2));check(!saved().contains("\"TD_A\""),"regular helper calls belong to baseline");
   Files.delete(regular);script("");
   // A module can contain a source file named like the managed file without being the managed root script.
   java.lang.reflect.Field providerField=minetweaker.runtime.MTTweaker.class.getDeclaredField("scriptProvider");providerField.setAccessible(true);
   final minetweaker.runtime.IScriptProvider provider=(minetweaker.runtime.IScriptProvider)providerField.get(minetweaker.MineTweakerAPI.tweaker);
   minetweaker.MineTweakerAPI.tweaker.setScriptProvider(()->{
    List<minetweaker.runtime.IScriptIterator> groups=new ArrayList<>();provider.getScripts().forEachRemaining(groups::add);
    groups.add(new minetweaker.runtime.IScriptIterator(){boolean first=true;
     public String getGroupName(){return "td-editor-export";}public String getName(){return ResearchEditor.FILE_NAME;}
     public boolean next(){boolean result=first;first=false;return result;}
     public java.io.InputStream open(){return new java.io.ByteArrayInputStream(helper.getBytes(StandardCharsets.UTF_8));}
    });return groups.iterator();
   });
   try {script("");ResearchEditor.edit("module baseline",l->l.setWarp("TD_C",2));
    check(thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==5&&!saved().contains("\"TD_A\""),"same-named source in another module is regular baseline");
   } finally {minetweaker.MineTweakerAPI.tweaker.setScriptProvider(provider);script("");}
   Files.write(regular,"mods.thaumicdabblery.ResearchEditor.move(\"TD_A\", \"TD_EDITOR\", 2, 0);\n".getBytes(StandardCharsets.UTF_8));script("");
   check(ResearchEditor.problem()!=null&&items.get("TD_A").displayColumn==0,"invalid regular editor batch is atomic");
   Files.write(regular,"mods.thaumicdabblery.ResearchEditor.warp(\"TD_A\", 5);\n".getBytes(StandardCharsets.UTF_8));script("mods.thaumicdabblery.ResearchEditor.warp(\"TD_A\", -1);\n");
   check(ResearchEditor.problem()!=null&&thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==5,"invalid managed batch preserves valid regular baseline");
  } finally {Files.deleteIfExists(regular);Files.deleteIfExists(second);script("");}
 }

}
