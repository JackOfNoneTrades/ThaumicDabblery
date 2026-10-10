package tdtest;

import java.lang.reflect.*;
import java.util.*;
import cpw.mods.fml.common.*;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.aspects.*;
import thaumcraft.api.research.*;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.common.lib.research.ResearchManager;
import org.fentanylsolutions.thaumicdabblery.feature.construct.*;

@Mod(modid="tdconstructclient",name="Construct page client probe",version="1",dependencies="required-after:thaumicdabblery",acceptableRemoteVersions="*")
public final class ConstructPageClientProbe {
 private final ConstructPageChecks c=new ConstructPageChecks();private int ticks,frames,stage;private boolean launched,done;private GuiResearchRecipe gui;
 private static Field field(String name)throws Exception{Field f=GuiResearchRecipe.class.getDeclaredField(name);f.setAccessible(true);return f;}
 @Mod.EventHandler public void ready(FMLLoadCompleteEvent e){FMLCommonHandler.instance().bus().register(this);}
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END||done||++ticks<30)return;Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;
  try{if(!launched){launched=true;ticks=0;String[] address=System.getProperty("td.construct.server").split(":");FMLClientHandler.instance().connectToServerAtStartup(address[0],Integer.parseInt(address[1]));return;}
   if(ticks>1000)throw new AssertionError("client timeout");if(gui!=null||ticks<100||mc.field_71439_g==null||mc.field_71441_e==null)return;
   c.baseline();ResearchItem r=ResearchCategories.getResearch("TD_PORTAL_CONSTRUCTS");c.check(ResearchManager.isResearchComplete(mc.field_71439_g.func_70005_c_(),r.key),"real player's demo automatically unlocked");gui=new GuiResearchRecipe(r,0,0,0);mc.func_147108_a(gui);
  }catch(Throwable t){finish(mc,t);}
 }
 @SubscribeEvent public void frame(TickEvent.RenderTickEvent e){if(e.phase!=TickEvent.Phase.END||done||gui==null||++frames<20)return;frames=0;Minecraft mc=Minecraft.func_71410_x();mc.field_71458_u.func_146257_b();
  try{
   c.check(mc.field_71462_r==gui,"stock GUI stays open");shot(mc,"construct-pages-"+stage+".png");
   ResearchPage[] pages=(ResearchPage[])field("pages").get(gui);int number=field("page").getInt(gui);
   int gx=(gui.field_146294_l-field("paneWidth").getInt(gui))/2,gy=(gui.field_146295_m-field("paneHeight").getInt(gui))/2;
   for(int i=number;i<Math.min(number+2,pages.length);i++){
    ConstructPage p=(ConstructPage)pages[i];int x=gx+(i-number)*152-14,y=gy-8+(i==0?25:0);
    int[] caps={GL11.GL_LIGHTING,GL11.GL_LIGHT0,GL11.GL_LIGHT1,GL11.GL_COLOR_MATERIAL,GL11.GL_DEPTH_TEST,GL11.GL_CULL_FACE,GL11.GL_BLEND,GL11.GL_ALPHA_TEST,GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL};boolean[] state=new boolean[caps.length];for(int j=0;j<caps.length;j++)state[j]=GL11.glIsEnabled(caps[j]);int a=GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH),m=GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
    field("tooltip").set(gui,null);ConstructPageRenderer.draw(gui,p,x,y,x+60,y+20);
    c.check((field("tooltip").get(gui)!=null)==(p.activation!=null),"activation hover only when present");
    for(int j=0;j<caps.length;j++)c.check(state[j]==GL11.glIsEnabled(caps[j]),"renderer preserves GL state "+caps[j]);c.check(a==GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH)&&m==GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH),"balanced GL stacks");
    if(p.cost.size()>0){field("tooltip").set(gui,null);ConstructPageRenderer.draw(gui,p,x,y,x+13,y+156);c.check(field("tooltip").get(gui)!=null,"cost aspect hover");}
   }
   if(stage==0){Method click=GuiResearchRecipe.class.getDeclaredMethod("func_73864_a",int.class,int.class,int.class);click.setAccessible(true);click.invoke(gui,gx+265,gy+193,0);c.check(field("page").getInt(gui)==2,"native page navigation");stage++;return;}
   if(stage==1){ConstructPage t=(ConstructPage)pages[2];Method variant=ConstructPageRenderer.class.getDeclaredMethod("variant",ConstructPage.class,ItemStack.class);variant.setAccessible(true);ItemStack shown=(ItemStack)variant.invoke(null,t,t.layers[1][0][0]);c.check(shown.func_77960_j()>=0&&shown.func_77960_j()<9&&t.layers[1][0][0].func_77960_j()==32767,"wildcard displays valid flower without changing recipe");
    ItemStack[][][] tall=new ItemStack[16][1][1];for(int j=0;j<16;j++)tall[j][0][0]=ConstructPageChecks.stack("stone");
    ItemStack[][][] water={{{ConstructPageChecks.stack("water"),ConstructPageChecks.stack("lava")}}};
    AspectList cost=new AspectList().add(Aspect.AIR,1).add(Aspect.FIRE,2).add(Aspect.WATER,3).add(Aspect.EARTH,4).add(Aspect.ORDER,5).add(Aspect.ENTROPY,6);
    ResearchItem edge=new ResearchItem("TD_CONSTRUCT_EDGE","BASICS",new AspectList(),90,90,0,ConstructPageChecks.stack("stone")).setPages(new ConstructPage(tall,null,new AspectList()),new ConstructPage(water,null,cost));
    gui=new GuiResearchRecipe(edge,0,0,0);mc.func_147108_a(gui);stage++;return;
   }
   liquidPixels(mc,"water");liquidPixels(mc,"lava");
   finish(mc,null);
  }catch(Throwable t){finish(mc,t);}
 }
 private void liquidPixels(Minecraft mc,String id)throws Exception{
  GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);int mode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
  GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glLoadIdentity();GL11.glOrtho(0,mc.field_71443_c,mc.field_71440_d,0,1000,3000);
  GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();GL11.glLoadIdentity();GL11.glTranslatef(0,0,-2000);
  try{
   GL11.glViewport(0,0,mc.field_71443_c,mc.field_71440_d);GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(96,mc.field_71440_d-144,48,48);
   GL11.glDepthMask(true);GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
   GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_CULL_FACE);GL11.glFrontFace(GL11.GL_CCW);GL11.glCullFace(GL11.GL_BACK);
   Method draw=ConstructPageRenderer.class.getDeclaredMethod("liquid",Minecraft.class,net.minecraft.block.Block.class,int.class,int.class,int.class);draw.setAccessible(true);
   net.minecraft.block.Block block=(net.minecraft.block.Block)net.minecraft.block.Block.field_149771_c.func_82594_a("minecraft:"+id);draw.invoke(null,mc,block,0,112,112);
   c.check(GL11.glIsEnabled(GL11.GL_CULL_FACE),"liquid restores culling");
   java.nio.ByteBuffer pixels=org.lwjgl.BufferUtils.createByteBuffer(48*48*4);GL11.glReadPixels(96,mc.field_71440_d-144,48,48,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
   int lit=0;for(int i=0;i<pixels.capacity();i+=4)if((pixels.get(i)&255)+(pixels.get(i+1)&255)+(pixels.get(i+2)&255)>30)lit++;
   c.check(lit>60,"visible liquid faces with model culling enabled: "+id);
  }finally{GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(mode);GL11.glPopAttrib();}
 }
 private void shot(Minecraft mc,String name){ScreenShotHelper.func_148259_a(new java.io.File("."),name,mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());}
 private void finish(Minecraft mc,Throwable t){done=true;System.out.println(t==null?"TD_CONSTRUCT_CLIENT_PASS checks="+c.checks:"TD_CONSTRUCT_CLIENT_FAILED checks="+c.checks);if(t!=null)t.printStackTrace();if(mc.field_71439_g!=null)mc.field_71439_g.func_71165_d("/stop");mc.func_71400_g();}
}
