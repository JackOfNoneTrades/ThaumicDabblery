package tdtest;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ScreenShotHelper;
import org.lwjgl.opengl.GL11;
import org.fentanylsolutions.thaumicdabblery.feature.planarvortex.*;
import thaumcraft.api.research.*;
import thaumcraft.client.gui.GuiResearchRecipe;

public final class PlanarVortexPageClientChecks {
 public int checks;private int ticks;private GuiResearchRecipe gui;
 private void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 private static Field field(String name)throws Exception{Field f=GuiResearchRecipe.class.getDeclaredField(name);f.setAccessible(true);return f;}
 private void shot(Minecraft mc,String name){ScreenShotHelper.func_148259_a(new java.io.File("."),name,mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());}
 public boolean tick(Minecraft mc)throws Exception{
  ticks++;mc.field_71458_u.func_146257_b();
  if(ticks==1){ResearchItem research=ResearchCategories.getResearch("planarRift");check(research.getPages().length==8,"eight scripted book pages synchronized");for(ResearchPage p:research.getPages())check(p instanceof VortexPage&&((VortexPage)p).resolve()!=null,"vortex page resolved on client");gui=new GuiResearchRecipe(research,0,0,0);mc.func_147108_a(gui);return false;}
  if(ticks%20!=0)return false;
  check(mc.field_71462_r==gui,"stock book GUI remains open");int number=field("page").getInt(gui);check(number==(ticks/20-1)*2,"native next-page navigation");shot(mc,"vortex-pages-"+number+".png");
  ResearchPage[] pages=(ResearchPage[])field("pages").get(gui);int x=(gui.field_146294_l-field("paneWidth").getInt(gui))/2,y=(gui.field_146295_m-field("paneHeight").getInt(gui))/2;
  VortexPage page=(VortexPage)pages[number];
  int[] caps={GL11.GL_CULL_FACE,GL11.GL_LIGHTING,GL11.GL_LIGHT0,GL11.GL_LIGHT1,GL11.GL_COLOR_MATERIAL,GL11.GL_DEPTH_TEST,GL11.GL_BLEND,GL11.GL_ALPHA_TEST,GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL};
  boolean[] enabled=new boolean[caps.length];for(int i=0;i<caps.length;i++)enabled[i]=GL11.glIsEnabled(caps[i]);int depth=GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH),matrix=GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
  int px=x-14,py=y-8+(number==0?25:0);
  field("tooltip").set(gui,null);VortexPageRenderer.draw(gui,page,px,py,px+60,py+20);
  check(field("tooltip").get(gui)!=null,"input hover schedules native tooltip");
  for(int i=0;i<caps.length;i++)check(enabled[i]==GL11.glIsEnabled(caps[i]),"page restores GL state "+caps[i]);check(depth==GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH)&&matrix==GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH),"page restores GL stacks");
  field("tooltip").set(gui,null);VortexPageRenderer.draw(gui,page,px,py,px+60,py+114);check(field("tooltip").get(gui)!=null,"output hover schedules native tooltip");
  field("tooltip").set(gui,null);VortexPageRenderer.draw(gui,page,px,py,px+20,py+54);check((field("tooltip").get(gui)!=null)==!"instant".equals(page.resolve().completion),"wand indicator appears only for wand activation");
  if(ticks==80){
   VortexRecipes.RECIPES.put("custom:page_wildcard",new VortexRecipes.Recipe(new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.field_150325_L,2,32767),new net.minecraft.item.ItemStack(net.minecraft.init.Items.field_151045_i),null,new net.minecraft.nbt.NBTTagCompound()));
   try{VortexPage wildcard=new VortexPage("custom:page_wildcard",null);field("tooltip").set(gui,null);VortexPageRenderer.draw(gui,wildcard,px,py,px+60,py+20);Object[] tip=(Object[])field("tooltip").get(gui);check(tip!=null&&((java.util.List)tip[0]).contains("Any item metadata"),"wildcard hover safely displays representative metadata");check(wildcard.resolve().input.func_77960_j()==32767,"wildcard rendering preserves real recipe input");}finally{VortexRecipes.RECIPES.remove("custom:page_wildcard");}
   checkModels(mc,pages,px,py);
   checkBlockLighting(mc,pages,px,py);
   mc.func_147108_a((GuiScreen)null);return true;
  }
  Method click=GuiResearchRecipe.class.getDeclaredMethod("func_73864_a",int.class,int.class,int.class);click.setAccessible(true);click.invoke(gui,x+265,y+193,0);return false;
 }
 private static Object member(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
 private static float[] renderState()throws Exception{
  net.minecraft.client.renderer.entity.RenderManager m=net.minecraft.client.renderer.entity.RenderManager.field_78727_a;
  return new float[]{m.field_78735_i,m.field_78732_j,net.minecraft.client.renderer.OpenGlHelper.class.getField("lastBrightnessX").getFloat(null),net.minecraft.client.renderer.OpenGlHelper.class.getField("lastBrightnessY").getFloat(null),
   net.minecraft.client.renderer.ActiveRenderInfo.field_74588_d,net.minecraft.client.renderer.ActiveRenderInfo.field_74589_e,net.minecraft.client.renderer.ActiveRenderInfo.field_74586_f,net.minecraft.client.renderer.ActiveRenderInfo.field_74587_g,net.minecraft.client.renderer.ActiveRenderInfo.field_74596_h};
 }
 private void checkModels(Minecraft mc,ResearchPage[] pages,int x,int y)throws Exception{
  Field cacheField=Class.forName("org.fentanylsolutions.thaumicdabblery.feature.planarvortex.VortexEntityPreview").getDeclaredField("CACHE");cacheField.setAccessible(true);
  Map cache=(Map)((Map)cacheField.get(null)).get(gui);
  for(int index:new int[]{1,2,4,5}){
   VortexPage page=(VortexPage)pages[index];int before=mc.field_71441_e.field_72996_f.size();float[] state=renderState();
   VortexPageRenderer.draw(gui,page,x,y,0,0);Object preview=cache.get(page);
   check(preview!=null&&!((Boolean)member(preview,"failed")),"entity preview renders: "+page.key);
   net.minecraft.entity.EntityLiving entity=(net.minecraft.entity.EntityLiving)member(preview,"entity");
   check(entity!=null&&!mc.field_71441_e.field_72996_f.contains(entity)&&before==mc.field_71441_e.field_72996_f.size(),"preview never spawned: "+page.key);
   check(Arrays.equals(state,renderState()),"preview restores lightmap, camera and billboard state: "+page.key);
   VortexPageRenderer.draw(gui,page,x,y,0,0);check(member(cache.get(page),"entity")==entity,"preview reused between frames: "+page.key);
   if(index==2)check(entity.func_70631_g_()&&"The Visitor".equals(entity.func_94057_bL()),"preview applies baby and custom-name NBT");
  }
  VortexPage override=new VortexPage(((VortexPage)pages[1]).key,new net.minecraft.item.ItemStack(net.minecraft.init.Items.field_151147_al));
  VortexPageRenderer.draw(gui,override,x,y,0,0);check(!cache.containsKey(override),"explicit item icon bypasses entity rendering");
 }

 private byte[] dirtPixels(Minecraft mc,boolean nativeItem)throws Exception{
  GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
  int mode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
  GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glLoadIdentity();GL11.glOrtho(0,mc.field_71443_c,mc.field_71440_d,0,1000,3000);
  GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();GL11.glLoadIdentity();GL11.glTranslatef(0,0,-2000);
  try{
   GL11.glViewport(0,0,mc.field_71443_c,mc.field_71440_d);
   GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(96,mc.field_71440_d-144,48,48);
   GL11.glDepthMask(true);GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
   GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_ALPHA_TEST);GL11.glDisable(GL11.GL_NORMALIZE);
   GL11.glDisable(org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL);GL11.glColor4f(1,1,1,1);
   net.minecraft.item.ItemStack dirt=new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.field_150346_d);
   if(nativeItem){
    // Match vanilla GuiContainer: scaled item normals and standard inventory lights.
    GL11.glTranslatef(0,0,100);GL11.glEnable(org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL);
    net.minecraft.client.renderer.RenderHelper.func_74520_c();GL11.glEnable(GL11.GL_DEPTH_TEST);
    new net.minecraft.client.renderer.entity.RenderItem().func_82406_b(mc.field_71466_p,mc.func_110434_K(),dirt,112,112);
   }else{
    Method draw=VortexPageRenderer.class.getDeclaredMethod("item",Minecraft.class,net.minecraft.item.ItemStack.class,int.class,int.class);draw.setAccessible(true);draw.invoke(null,mc,dirt,112,112);
   }
   java.nio.ByteBuffer pixels=org.lwjgl.BufferUtils.createByteBuffer(48*48*4);
   GL11.glReadPixels(96,mc.field_71440_d-144,48,48,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
   byte[] result=new byte[pixels.capacity()];pixels.get(result);return result;
  }finally{
   GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(mode);GL11.glPopAttrib();
  }
 }
 private void checkBlockLighting(Minecraft mc,ResearchPage[] pages,int x,int y)throws Exception{
  byte[] expected=dirtPixels(mc,true);int lit=0;for(int i=0;i<expected.length;i+=4)if((expected[i]&255)>20)lit++;
  check(lit>50,"native dirt reference contains lit block pixels");
  check(Arrays.equals(expected,dirtPixels(mc,false)),"dirt matches vanilla inventory lighting with rescaling initially disabled");
  for(int index:new int[]{1,2,4,5}){
   VortexPageRenderer.draw(gui,(VortexPage)pages[index],x,y,0,0);
   check(Arrays.equals(expected,dirtPixels(mc,false)),"dirt lighting unchanged after creature preview "+index);
  }
 }

}
