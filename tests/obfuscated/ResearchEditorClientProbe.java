package tdtest;

import cpw.mods.fml.common.*;
import minetweaker.MineTweakerImplementationAPI;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.*;
import java.lang.reflect.*;
import java.util.*;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.world.*;
import org.fentanylsolutions.thaumicdabblery.feature.researcheditor.*;
import org.fentanylsolutions.thaumicdabblery.feature.scanall.ScanAllCommand;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.*;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.common.Thaumcraft;

@Mod(modid="tdeditorclientprobe",name="Research editor client probe",version="1",dependencies="required-after:thaumicdabblery;required-after:modtweaker2;after:tc4tweak;after:salisarcana")
public final class ResearchEditorClientProbe {
 private final ResearchEditorChecks checks=new ResearchEditorChecks();
 private int ticks,frames,stage;private boolean launched,done;private volatile boolean serverDone;private volatile Throwable failure;
 private String knowledge;private KeyBinding key;
 @Mod.EventHandler public void ready(FMLLoadCompleteEvent event){FMLCommonHandler.instance().bus().register(this);}
 @Mod.EventHandler public void started(FMLServerStartedEvent event){try{checks.setup();}catch(Throwable t){failure=t;}finally{serverDone=true;}}
 private Object field(Object obj,String name)throws Exception{Class<?> c=obj instanceof Class?(Class<?>)obj:obj.getClass();Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(obj instanceof Class?null:obj);}
 private void set(Object obj,String name,Object value)throws Exception{Class<?> c=obj instanceof Class?(Class<?>)obj:obj.getClass();Field f=c.getDeclaredField(name);f.setAccessible(true);f.set(obj instanceof Class?null:obj,value);}
 private Object call(Object screen,String normal,String srg,Class<?>[] types,Object... args)throws Exception{
  Method m;try{m=screen.getClass().getDeclaredMethod(normal,types);}catch(NoSuchMethodException e){m=screen.getClass().getDeclaredMethod(srg,types);}m.setAccessible(true);return m.invoke(screen,args);
 }
 private Object overlay()throws Exception {Minecraft mc=Minecraft.func_71410_x();ResearchEditorClient.beginFrame((GuiResearchBrowser)mc.field_71462_r);return field(ResearchEditorClient.class,"overlay");}
 private void mouse(int x,int y,int button,boolean down)throws Exception {
  Minecraft mc=Minecraft.func_71410_x();GuiScreen screen=mc.field_71462_r;
  String[] names={"event_x","event_y","eventButton","eventState","event_dwheel"};Object[] old=new Object[names.length];for(int i=0;i<names.length;i++)old[i]=field(Mouse.class,names[i]);
  try {set(Mouse.class,"event_x",x*mc.field_71443_c/screen.field_146294_l);set(Mouse.class,"event_y",(screen.field_146295_m-y-1)*mc.field_71440_d/screen.field_146295_m);set(Mouse.class,"eventButton",button);set(Mouse.class,"eventState",down);set(Mouse.class,"event_dwheel",0);screen.func_146274_d();}
  finally{for(int i=0;i<names.length;i++)set(Mouse.class,names[i],old[i]);}
 }
 private void click(int x,int y,int b)throws Exception{mouse(x,y,b,true);}
 private void release(int x,int y)throws Exception{mouse(x,y,0,false);}
 private int sx(int x)throws Exception{return (Integer)call(overlay(),"screenX","screenX",new Class[]{int.class},x);}
 private int sy(int y)throws Exception{return (Integer)call(overlay(),"screenY","screenY",new Class[]{int.class},y);}
 private void menu(int row)throws Exception{Object g=overlay();click((Integer)field(g,"menuX")+12,(Integer)field(g,"menuY")+34+row*16,0);}
 private void toolbar(String button)throws Exception{Object g=overlay();int x=button.equals("Done")?(Integer)field(g,"right")-45:button.equals("Undo")?(Integer)field(g,"left")+40:(Integer)field(g,"left")+90;int y=(Integer)field(g,"toolbarY")+6;click(x,y,0);if(ResearchEditorClient.enabled())release(x,y);}
 private void type(int key)throws Exception{type(key,' ');}
 private void type(int key,char character)throws Exception{
  Object event=field(Keyboard.class,"current_event");String[] names={"key","state","character"};Object[] old=new Object[names.length];for(int i=0;i<names.length;i++)old[i]=field(event,names[i]);
  try{set(event,"key",key);set(event,"state",true);set(event,"character",(int)character);Minecraft.func_71410_x().field_71462_r.func_146282_l();}finally{for(int i=0;i<names.length;i++)set(event,names[i],old[i]);}
 }
 private void shortcut(int key,boolean shift)throws Exception {
  java.nio.ByteBuffer keys=(java.nio.ByteBuffer)field(Keyboard.class,"keyDownBuffer");int[] codes={29,219,42};byte[] old=new byte[codes.length];for(int i=0;i<codes.length;i++){old[i]=keys.get(codes[i]);keys.put(codes[i],(byte)(i==2&&!shift?0:1));}
  try{type(key);}finally{for(int i=0;i<codes.length;i++)keys.put(codes[i],old[i]);}
 }
 private void completionCounter(boolean expectVisible)throws Exception {
  if(!Loader.isModLoaded("tc4tweak"))return;
  Minecraft mc=Minecraft.func_71410_x();
  Class<?> config=Class.forName("net.glease.tc4tweak.ConfigurationHandler");Object settings=config.getField("INSTANCE").get(null);
  Object previous=field(settings,"counterStyle");Class<?> style=Class.forName("net.glease.tc4tweak.ConfigurationHandler$CompletionCounterStyle");
  Method draw=Class.forName("net.glease.tc4tweak.modules.researchBrowser.DrawResearchCompletionCounter").getDeclaredMethod("drawCompletionCounter",GuiResearchBrowser.class,int.class,int.class,int.class,int.class);draw.setAccessible(true);
  org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
  try{
   set(settings,"counterStyle",style.getField("All").get(null));
   mc.func_147110_a().func_147610_a(false);mc.field_71460_t.func_78478_c();
   org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
   org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
   org.lwjgl.opengl.GL11.glClearColor(0,0,0,1);org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT);
   java.nio.ByteBuffer before=org.lwjgl.BufferUtils.createByteBuffer(mc.field_71443_c*mc.field_71440_d*4),after=org.lwjgl.BufferUtils.createByteBuffer(before.capacity());
   org.lwjgl.opengl.GL11.glReadPixels(0,0,mc.field_71443_c,mc.field_71440_d,org.lwjgl.opengl.GL11.GL_RGBA,org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE,before);
   draw.invoke(null,(GuiResearchBrowser)mc.field_71462_r,10,10,-100,-100);
   org.lwjgl.opengl.GL11.glReadPixels(0,0,mc.field_71443_c,mc.field_71440_d,org.lwjgl.opengl.GL11.GL_RGBA,org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE,after);
   checks.check(before.equals(after)!=expectVisible,expectVisible?"completion counter renders again after Done":"completion counter draws no pixels over edit mode header");
   Class<?> search=Class.forName("net.glease.tc4tweak.modules.researchBrowser.ThaumonomiconIndexSearcher");
   search.getMethod("onGuiPostDraw",net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Post.class).invoke(search.getField("instance").get(null),new net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Post(mc.field_71462_r,-100,-100,0));
   java.nio.ByteBuffer afterSearch=org.lwjgl.BufferUtils.createByteBuffer(after.capacity());
   org.lwjgl.opengl.GL11.glReadPixels(0,0,mc.field_71443_c,mc.field_71440_d,org.lwjgl.opengl.GL11.GL_RGBA,org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE,afterSearch);
   checks.check(after.equals(afterSearch)!=expectVisible,expectVisible?"search field renders again after Done":"search field draws no pixels over editor save status");
  }finally{set(settings,"counterStyle",previous);org.lwjgl.opengl.GL11.glPopAttrib();}
 }
 private void shot(String name){Minecraft m=Minecraft.func_71410_x();net.minecraft.util.ScreenShotHelper.func_148259_a(new java.io.File("."),name+".png",m.field_71443_c,m.field_71440_d,m.func_147110_a());}
 private void tab(String tab)throws Exception{Object g=overlay();int tries=0;ResearchEditorClient.turnPage(-10000);while(!ResearchEditorClient.visibleTabs().contains(tab)&&tries++<20)ResearchEditorClient.turnPage(1);int index=new ArrayList<>(ResearchEditorClient.visibleTabs()).indexOf(tab),count=ResearchEditorClient.tabsPerSide();int x=index<count?(Integer)field(g,"left")-28:(Integer)field(g,"right")+28;int y=(Integer)field(g,"top")-5+(index%count)*24;click(x,y,0);}
 private String knowledge(){String player=Minecraft.func_71410_x().field_71439_g.func_70005_c_();return String.valueOf(Thaumcraft.proxy.getPlayerKnowledge().researchCompleted.get(player))+Thaumcraft.proxy.getPlayerKnowledge().getAspectsDiscovered(player).aspects.toString();}
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent event){
  if(event.phase!=TickEvent.Phase.END||done||++ticks<30)return;Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;
  try{
   if(!launched){launched=true;ticks=0;mc.func_71371_a("td_editor_"+System.currentTimeMillis(),"Research editor",new WorldSettings(42,WorldSettings.GameType.CREATIVE,false,false,WorldType.field_77138_c).func_77166_b());return;}
   if(failure!=null)throw new AssertionError("integrated setup",failure);
   if(ticks>1800)throw new AssertionError("client timeout stage "+stage);
   if(!serverDone||mc.field_71439_g==null||ticks<100)return;
   if(stage>0&&frames<6)return;frames=0;
   if(stage==0){
    key=(KeyBinding)field(ResearchEditorClient.class,"KEY");checks.check(key.func_151463_i()==0,"key defaults unbound");
    checks.check(!ResearchEditorClient.enabled(),"editor defaults off");
    new ScanAllCommand().func_71515_b(mc.field_71439_g,new String[]{"edit"});checks.check(ResearchEditorClient.enabled(),"command toggles while book is closed");
    set(GuiResearchBrowser.class,"selectedCategory",ResearchEditorChecks.TAB);
    GuiResearchBrowser.completedResearch.put(mc.field_71439_g.func_70005_c_(),new ArrayList<String>());
    Thaumcraft.proxy.getPlayerKnowledge().getAspectsDiscovered(mc.field_71439_g.func_70005_c_()).add(Aspect.ORDER,123);
    knowledge=knowledge();mc.func_147108_a(new GuiResearchBrowser());checks.check(mc.field_71462_r.getClass()==GuiResearchBrowser.class&&ResearchEditorClient.enabled(),"editor uses exact stock browser class");
   }else if(stage==1){
    completionCounter(false);
    call(overlay(),"focus","focus",new Class[]{String.class},"TD_A");
    shot("editor-canvas");click(sx(0),sy(0),0);release(sx(0),sy(0));checks.check(knowledge.equals(knowledge()),"click neither completes research nor spends points");
    int x=sx(0),y=sy(0);click(x,y,0);mouse(x+24,y+24,-1,false);
    ResearchBrowserAccess geo=(ResearchBrowserAccess)mc.field_71462_r;double camera=geo.thaumicdabblery$mapX();
    java.nio.ByteBuffer buttons=(java.nio.ByteBuffer)field(Mouse.class,"buttons");byte prior=buttons.get(0);buttons.put(0,(byte)1);
    try{mc.field_71462_r.func_73863_a(x,y,0);mc.field_71462_r.func_73863_a(x+12,y+12,0);}finally{buttons.put(0,prior);}
    checks.check(geo.thaumicdabblery$mapX()==camera,"native drag polling does not pan while repositioning research");
   }else if(stage==2){
    shot("editor-drag");release(sx(1),sy(1));checks.check(checks.items.get("TD_A").displayColumn==1&&checks.items.get("TD_A").displayRow==1,"drag commits snapped position");
    toolbar("Undo");checks.check(checks.items.get("TD_A").displayColumn==0,"undo button restores drag");
    toolbar("Redo");checks.check(checks.items.get("TD_A").displayColumn==1,"redo button repeats drag");
    click(sx(1),sy(1),1);menu(3);
   }else if(stage==3){
    shot("editor-properties");Object menuOverlay=overlay();click((Integer)field(menuOverlay,"menuX")+12,(Integer)field(menuOverlay,"menuY")+34,1);checks.check(ResearchEditor.layout().require("TD_A").flags==0,"right-click does not execute a context-menu item");for(int i=0;i<6;i++)menu(i);type(1);
    checks.check(ResearchEditor.layout().require("TD_A").flags==63&&checks.items.get("TD_A").isVirtual(),"all six property controls work together");
    shortcut(44,false);checks.check(ResearchEditor.layout().require("TD_A").flags==31&&!checks.items.get("TD_A").isVirtual(),"Ctrl/Cmd+Z undoes Virtual");
    shortcut(44,true);checks.check(ResearchEditor.layout().require("TD_A").flags==63&&checks.items.get("TD_A").isVirtual(),"Ctrl/Cmd+Shift+Z redoes Virtual");
    click(sx(1),sy(1),1);menu(3);menu(5);type(1);
    checks.check(!checks.items.get("TD_A").isVirtual(),"Virtual checkbox clears flag");
    click(sx(1),sy(1),1);menu(3);menu(6);type(6,'5');type(28);
    checks.check(thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==5&&field(overlay(),"warpKey")==null,"forbidden knowledge input applies entered amount");
    toolbar("Undo");checks.check(thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==0,"warp UI undo");
    toolbar("Redo");checks.check(thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==5,"warp UI redo");
    click(sx(1),sy(1),1);menu(3);menu(6);type(12,'-');type(28);
    checks.check(field(overlay(),"warpKey")!=null&&thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==5,"invalid warp input stays open without applying");type(1);
    click(sx(1),sy(1),1);menu(3);menu(6);type(2,'1');
   }else if(stage==4){
    Object input=field(overlay(),"warpInput");int cursor=(Integer)field(input,"field_146214_l");
    for(int i=0;i<100;i++)ResearchEditorClient.beginFrame((GuiResearchBrowser)mc.field_71462_r);
    checks.check((Integer)field(input,"field_146214_l")==cursor,"render and input preparation do not accelerate cursor blink");
    ResearchEditorClient handler=new ResearchEditorClient();handler.tick(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
    checks.check((Integer)field(input,"field_146214_l")==cursor,"start phase does not double-tick cursor");
    handler.tick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
    checks.check((Integer)field(input,"field_146214_l")==cursor+1,"cursor advances once per client tick");
    shot("editor-forbidden-knowledge");type(1);
    checks.check(thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==5,"Escape cancels unapplied warp value");
    Object failed=overlay();
    call(failed,"failure","failure",new Class[]{Exception.class},new java.nio.file.FileSystemException("scripts/.research-editor-test-123.tmp","scripts/thaumicdabblery_research_editor.zs","TD_TEST_SHARING_VIOLATION"));
    checks.check("Save failed".equals(field(failed,"status"))&&((String)field(failed,"notice")).contains("game log")&&!((String)field(failed,"notice")).contains(".tmp"),"filesystem errors show a short actionable message");
    call(failed,"saved","saved",new Class[]{String.class},"Diagnostic test finished");
    ResearchEditor.edit("occupy destination",l->l.move("TD_D",ResearchEditorChecks.OTHER,1,1));
    checks.check(field(mc.field_71462_r,"currentHighlight")==null,"native purchase and creative hover target stays empty");
    click(sx(1),sy(1),1);menu(1);
   }else if(stage==5){
    shot("editor-select-tab");tab(ResearchEditorChecks.OTHER);checks.check(checks.items.get("TD_A").category.equals(ResearchEditorChecks.OTHER),"tab click moves selected entry into occupied destination tab");
    checks.check(checks.items.get("TD_A").displayColumn==0&&checks.items.get("TD_A").displayRow==0,"occupied destination relocates to first free cell");
    checks.check(field(overlay(),"tab").equals(ResearchEditorChecks.OTHER),"move focuses destination tab");
    click(sx(0),sy(0),1);menu(2);tab(ResearchEditorChecks.TAB);click(sx(2),sy(0),0);
    checks.check(checks.items.get("TD_A").category.equals(ResearchEditorChecks.TAB)&&checks.items.get("TD_B").category.equals(ResearchEditorChecks.OTHER),"cross-tab swap target selection");
    toolbar("Undo");checks.check(checks.items.get("TD_B").category.equals(ResearchEditorChecks.TAB),"swap is single undo action");
    click(sx(2),sy(0),1);menu(0);menu(0);tab(ResearchEditorChecks.OTHER);
   }else if(stage==6){
    shot("editor-select-parent");click(sx(1),sy(1),0);checks.check(Arrays.equals(checks.items.get("TD_B").parents,new String[]{"TD_D"}),"parent picker works across tabs");
    tab(ResearchEditorChecks.TAB);click(sx(2),sy(0),1);menu(0);menu(1);menu(2);type(1);
    checks.check(Arrays.equals(checks.items.get("TD_B").parentsHidden,new String[]{"TD_D"}),"parent context hidden-link control");
    click(sx(2),sy(0),1);menu(4);checks.check(ResearchCategories.getResearch("TD_B")==null&&checks.items.get("TD_C").parentsHidden.length==0,"delete UI detaches hidden links");
    toolbar("Undo");checks.check(ResearchCategories.getResearch("TD_B")==checks.items.get("TD_B")&&checks.items.get("TD_C").parentsHidden.length==1,"delete undo restores all ties");
    Object oldWheel=field(Mouse.class,"dwheel");java.nio.ByteBuffer keys=(java.nio.ByteBuffer)field(Keyboard.class,"keyDownBuffer");byte control=keys.get(29);keys.put(29,(byte)1);set(Mouse.class,"dwheel",120);String priorTab=ResearchEditorClient.selectedTab();
    try{mc.field_71462_r.func_146269_k();}finally{keys.put(29,control);set(Mouse.class,"dwheel",oldWheel);}
    checks.check(ResearchEditorClient.selectedTab().equals(priorTab),"Salis post-input wheel shortcut cannot switch tabs twice");
    checks.check(knowledge.equals(knowledge()),"all editor actions preserve player knowledge and points");
    type(1);checks.check(mc.field_71462_r==null&&ResearchEditorClient.enabled(),"closing book preserves toggle state");mc.func_147108_a(new GuiResearchBrowser());checks.check(mc.field_71462_r.getClass()==GuiResearchBrowser.class&&ResearchEditorClient.enabled(),"reopen resumes editor");
    toolbar("Done");checks.check(mc.field_71462_r instanceof GuiResearchBrowser&&!ResearchEditorClient.enabled(),"Done returns to normal book and disables editing");
   }else if(stage==7){
    shot("editor-native-after");completionCounter(true);GuiScreen stock=mc.field_71462_r;
    key.func_151462_b(68);type(68);checks.check(mc.field_71462_r==stock&&ResearchEditorClient.enabled(),"bound key edits the same stock GUI instance");
    type(68);checks.check(mc.field_71462_r==stock&&!ResearchEditorClient.enabled(),"bound key leaves same stock GUI instance");
    key.func_151462_b(-99);click(10,10,1);checks.check(ResearchEditorClient.enabled()&&mc.field_71462_r==stock,"mouse binding works before Salis right-click handler");
    click(10,10,1);checks.check(!ResearchEditorClient.enabled()&&mc.field_71462_r==stock,"mouse binding switches editing off");
    key.func_151462_b(68);mc.func_147108_a(new GuiResearchRecipe(checks.items.get("TD_B"),0,0,0));type(68);
    checks.check(mc.field_71462_r.getClass()==GuiResearchBrowser.class&&ResearchEditorClient.enabled(),"key on a recipe page returns to stock map for editing");
    type(68);key.func_151462_b(0);
    new ScanAllCommand().func_71515_b(mc.field_71439_g,new String[]{"edit"});mc.func_147108_a(new GuiResearchBrowser());
    mc.field_71462_r.func_146280_a(mc,320,240);
   }else if(stage==8){
    shot("editor-small-screen");checks.check(mc.field_71462_r.getClass()==GuiResearchBrowser.class&&ResearchEditorClient.enabled(),"minimum-size editor renders");
    checks.check(ResearchEditor.undoName()!=null,"history survives closing book");
    for(int i=0;i<35;i++)ResearchCategories.registerCategory("TD_EXTRA"+i,new net.minecraft.util.ResourceLocation("thaumcraft","textures/aspects/ordo.png"),new net.minecraft.util.ResourceLocation("thaumcraft","textures/gui/gui_researchback.png"));
    Object g=overlay();Set<String> page=ResearchEditorClient.visibleTabs();click((Integer)field(g,"right")-9,(Integer)field(g,"toolbarY")+6,0);checks.check(!page.equals(ResearchEditorClient.visibleTabs()),"native tab page arrow works with many tabs");
    tab("TD_EXTRA34");checks.check("TD_EXTRA34".equals(ResearchEditorClient.selectedTab()),"last tab page remains selectable");mc.field_71462_r.func_73863_a(0,0,0);tab(ResearchEditorChecks.TAB);
    // Bottom tabs can overlap the toolbar vertically. Their outer edge must still select/move tabs.
    ResearchEditorClient.turnPage(-10000);
    int last=ResearchEditorClient.tabsPerSide()-1;
    String edgeTab=new ArrayList<>(ResearchEditorClient.visibleTabs()).get(last);
    int tabTop=(Integer)field(g,"top")-17+last*24,toolbarY=(Integer)field(g,"toolbarY");
    if(toolbarY>=tabTop&&toolbarY<tabTop+24){
     click(sx(0),sy(2),1);menu(1);ResearchEditorClient.turnPage(-10000);
     click((Integer)field(g,"left")-28,toolbarY,0);
     checks.check(checks.items.get("TD_C").category.equals(edgeTab),"tab click in toolbar-height band moves research");
     checks.check(field(g,"tab").equals(edgeTab),"tab-edge move focuses destination");
     toolbar("Undo");
    }
    MineTweakerImplementationAPI.reload();
   }else{
    checks.check(ResearchEditor.undoName()==null,"reload clears UI history");checks.check(checks.items.get("TD_A").category.equals(ResearchEditorChecks.OTHER),"saved file replay restores GUI changes");
    checks.check(thaumcraft.api.ThaumcraftApi.getWarp("TD_A")==5,"saved forbidden knowledge survives client reload");
    System.out.println("TD_EDITOR_CLIENT_PASS checks="+checks.checks);done=true;mc.func_71400_g();
   }
   stage++;
  }catch(Throwable t){done=true;System.out.println("TD_EDITOR_CLIENT_FAILED checks="+checks.checks+" stage="+stage);t.printStackTrace();mc.func_71400_g();}
 }
 @SubscribeEvent public void render(TickEvent.RenderTickEvent event){if(event.phase==TickEvent.Phase.END)frames++;}
}
