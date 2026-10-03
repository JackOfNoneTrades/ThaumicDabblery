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
  make("TD_V",OTHER,4,4);items.get("TD_V").setVirtual();
  script("");
 }
 private void make(String key,String tab,int x,int y){ResearchItem r=new ResearchItem(key,tab,new AspectList().add(Aspect.ORDER,1),x,y,1,new ResourceLocation("thaumcraft","textures/aspects/ordo.png"));r.setPages(new ResearchPage("Editor test"));r.registerResearchItem();items.put(key,r);}
 public void run()throws Exception {
  check(Boolean.FALSE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment")),"production runtime");
  setup();
  collectedScripts();
  exportedBatches();
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
   ResearchEditor.edit("first batch",l->{l.swap("TD_A","TD_B");l.toggleFlag("TD_A",1);l.setWarp("TD_A",7);l.parent("TD_A",null,"TD_D",true);l.delete("TD_V");});
   String exported=saved();Files.write(regular,exported.getBytes(StandardCharsets.UTF_8));script("");
   check(ResearchEditor.problem()==null&&items.get("TD_A").displayColumn==2&&items.get("TD_B").displayColumn==0,"exported occupied swap applies as a batch");
   check(items.get("TD_A").isHidden()&&thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==7&&Arrays.equals(items.get("TD_A").parentsHidden,new String[]{"TD_D"})&&ResearchCategories.getResearch("TD_V")==null,"export retains flags, warp, parents and deletion");
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
   check(items.get("TD_A").displayColumn==0&&items.get("TD_B").displayColumn==2&&!items.get("TD_A").isHidden()&&items.get("TD_A").parentsHidden==null&&thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==0&&ResearchCategories.getResearch("TD_V")==items.get("TD_V"),"removing exports restores original layout and deleted entries");
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
