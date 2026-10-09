package tdtest;
import java.util.*;
import java.lang.reflect.*;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import thaumic.tinkerer.client.gui.GuiEnchanting;
import thaumic.tinkerer.client.gui.button.GuiButtonEnchantment;
import thaumic.tinkerer.common.block.tile.TileEnchanter;
import thaumic.tinkerer.common.network.packet.*;
import thaumic.tinkerer.common.ThaumicTinkerer;
import org.fentanylsolutions.thaumicdabblery.feature.osmotic.OsmoticRecipes;
@Mod(modid="tdosmoticremote",name="Osmotic remote",version="1",acceptableRemoteVersions="*",dependencies="required-after:thaumicdabblery;required-after:ThaumicTinkerer;required-after:modtweaker2")
public final class OsmoticRemoteProbe {
 private int ticks,wait,stage,checks,chosen,firstStartY,hoverCapture;private boolean launched,done,captureRequested;private Set<Integer> first;
 private void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 @Mod.EventHandler public void init(FMLPostInitializationEvent e){OsmoticChecks.register();FMLCommonHandler.instance().bus().register(this);}
 private List<GuiButton> buttons(GuiEnchanting gui)throws Exception{Field f=GuiScreen.class.getDeclaredField("field_146292_n");f.setAccessible(true);return (List<GuiButton>)f.get(gui);}
 private Set<Integer> ids(GuiEnchanting gui)throws Exception{Set<Integer> out=new HashSet<>();for(GuiButton b:buttons(gui))if(b.field_146127_k>=1&&b.field_146127_k<=16&&b.field_146124_l)out.add(((GuiButtonEnchantment)b).enchant.field_77352_x);return out;}
 private void press(GuiEnchanting gui,int id)throws Exception{for(GuiButton b:buttons(gui))if(b.field_146127_k==id){check(b.field_146124_l,"button enabled "+id);Method m=GuiEnchanting.class.getDeclaredMethod("func_146284_a",GuiButton.class);m.setAccessible(true);m.invoke(gui,b);return;}throw new AssertionError("Missing button "+id);}
 private int[] renderState(){
  int[] keys={org.lwjgl.opengl.GL11.GL_LIGHTING,org.lwjgl.opengl.GL11.GL_LIGHT0,org.lwjgl.opengl.GL11.GL_LIGHT1,org.lwjgl.opengl.GL11.GL_COLOR_MATERIAL,org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL,org.lwjgl.opengl.GL11.GL_DEPTH_TEST,org.lwjgl.opengl.GL11.GL_BLEND,org.lwjgl.opengl.GL11.GL_ALPHA_TEST,org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D,org.lwjgl.opengl.GL11.GL_BLEND_SRC,org.lwjgl.opengl.GL11.GL_BLEND_DST,org.lwjgl.opengl.GL11.GL_SHADE_MODEL,org.lwjgl.opengl.GL11.GL_ATTRIB_STACK_DEPTH};
  int[] out=new int[keys.length+4];for(int i=0;i<keys.length;i++)out[i]=org.lwjgl.opengl.GL11.glGetInteger(keys[i]);
  java.nio.FloatBuffer color=org.lwjgl.BufferUtils.createFloatBuffer(16);org.lwjgl.opengl.GL11.glGetFloat(org.lwjgl.opengl.GL11.GL_CURRENT_COLOR,color);for(int i=0;i<4;i++)out[keys.length+i]=Float.floatToIntBits(color.get(i));return out;
 }
 private void tooltipState(GuiEnchanting gui)throws Exception{
  org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
  try{
   net.minecraft.client.renderer.RenderHelper.func_74519_b();
   org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL);org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_ALPHA_TEST);org.lwjgl.opengl.GL11.glShadeModel(org.lwjgl.opengl.GL11.GL_SMOOTH);org.lwjgl.opengl.GL11.glColor4f(.7F,.6F,.5F,.8F);
   int[] before=renderState();gui.tooltip=new ArrayList<>(Arrays.asList("Tooltip state check","Second line"));
   Method draw=GuiEnchanting.class.getDeclaredMethod("func_146979_b",int.class,int.class);draw.setAccessible(true);draw.invoke(gui,20,20);
   org.fentanylsolutions.thaumicdabblery.feature.osmotic.OsmoticTooltips.render(gui);
   check(Arrays.equals(before,renderState()),"native tooltip restores lighting, depth, blending, color, texture and attribute stack");check(gui.tooltip.isEmpty(),"native tooltip lifecycle remains intact");
  }finally{org.lwjgl.opengl.GL11.glPopAttrib();}
 }
 private void shot(Minecraft mc,String name){net.minecraft.util.ScreenShotHelper.func_148259_a(new java.io.File("."),name,mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());}
 @SubscribeEvent public void render(TickEvent.RenderTickEvent e){
  if(e.phase!=TickEvent.Phase.END||!captureRequested||hoverCapture>=2)return;
  Minecraft mc=Minecraft.func_71410_x();if(!(mc.field_71462_r instanceof GuiEnchanting))return;GuiEnchanting gui=(GuiEnchanting)mc.field_71462_r;
  try{int x=10,y=10;if(hoverCapture==1){GuiButton b=buttons(gui).stream().filter(v->v.field_146127_k==8).findFirst().get();x=b.field_146128_h+8;y=b.field_146129_i+8;}
   mc.field_71460_t.func_78478_c();gui.func_73863_a(x,y,e.renderTickTime);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Post(gui,x,y,e.renderTickTime));shot(mc,hoverCapture==0?"osmotic-nei-unhovered.png":"osmotic-nei-hovered.png");hoverCapture++;
  }catch(Exception ex){throw new RuntimeException(ex);}
 }
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END||done||++ticks<30)return;Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;try{
  if(!launched){launched=true;cpw.mods.fml.client.FMLClientHandler.instance().connectToServerAtStartup("127.0.0.1",Integer.getInteger("td.osmotic.port",25681));return;}
  if(mc.field_71462_r instanceof net.minecraft.client.gui.GuiDisconnected)throw new AssertionError("Disconnected");if(ticks>1300)throw new AssertionError("Timeout stage="+stage);if(!(mc.field_71462_r instanceof GuiEnchanting))return;GuiEnchanting gui=(GuiEnchanting)mc.field_71462_r;TileEnchanter tile=gui.enchanter;if(++wait<40)return;wait=0;
  if(stage==0&&hoverCapture<2){captureRequested=true;return;}
  if(stage==0){tooltipState(gui);check(!mc.func_71356_B(),"real dedicated server");GuiButton start=buttons(gui).stream().filter(b->b.field_146127_k==0).findFirst().get();firstStartY=start.field_146129_i;check(start.field_146125_m&&!start.field_146124_l&&start instanceof org.fentanylsolutions.thaumicdabblery.feature.osmotic.OsmoticStartButton,"start symbol present and disabled before selection");start.func_146112_a(mc,start.field_146128_h+2,start.field_146129_i+2);check(gui.tooltip.contains("Select an enchantment to begin"),"disabled control explains how to start");gui.tooltip.clear();first=ids(gui);check(first.size()==16&&!first.contains(16),"first page full and research-gated enchantment hidden");shot(mc,"osmotic-page-1.png");press(gui,-30999);stage++;return;}
  if(stage==1){Set<Integer> second=ids(gui);check(!second.isEmpty()&&Collections.disjoint(first,second),"second page exposes distinct entries");check(second.size()<=8&&buttons(gui).stream().filter(b->b.field_146127_k==0).findFirst().get().field_146129_i==firstStartY+24,"start symbol follows the single row down by 24 pixels");shot(mc,"osmotic-page-2.png");chosen=second.stream().filter(OsmoticChecks.custom::contains).findFirst().get();ThaumicTinkerer.netHandler.sendToServer(new PacketEnchanterAddEnchant(tile,chosen,0));stage++;return;}
  if(stage==2){check(tile.enchantments.contains(chosen)&&tile.levels.get(0)==1,"native selection packet accepted");check(tile.totalAspects.getAmount(thaumcraft.api.aspects.Aspect.AIR)==6,"base cost synchronized");check(buttons(gui).stream().anyMatch(b->b.field_146127_k==-30998&&b.field_146126_j.startsWith("2/")),"page retained after server synchronizes selection");check(OsmoticRecipes.data(chosen,1).texture.toString().equals(OsmoticChecks.ICON),"scripted icon synchronized");ThaumicTinkerer.netHandler.sendToServer(new PacketEnchanterAddEnchant(tile,chosen,3));stage++;return;}
  if(stage==3){check(tile.levels.get(0)==3&&tile.totalAspects.getAmount(thaumcraft.api.aspects.Aspect.AIR)==24,"level change and cost synchronized");ThaumicTinkerer.netHandler.sendToServer(new PacketEnchanterAddEnchant(tile,-999,0));ThaumicTinkerer.netHandler.sendToServer(new PacketEnchanterAddEnchant(tile,chosen,999));ThaumicTinkerer.netHandler.sendToServer(new PacketEnchanterAddEnchant(tile,16,0));stage++;return;}
  if(stage==4){check(tile.enchantments.equals(Arrays.asList(chosen))&&tile.levels.get(0)==3,"server rejects malformed and research-locked selections");mc.field_71439_g.func_71165_d("/tdosmotic remove "+chosen);stage++;return;}
  if(stage==5){check(tile.enchantments.isEmpty()&&tile.totalAspects.size()==0,"reload clears removed selection on separate client");check(OsmoticRecipes.data(chosen,1)==null&&!ids(gui).contains(chosen),"removed recipe disappears from open GUI");chosen=ids(gui).stream().filter(OsmoticChecks.custom::contains).findFirst().get();ThaumicTinkerer.netHandler.sendToServer(new PacketEnchanterAddEnchant(tile,chosen,0));if(buttons(gui).stream().anyMatch(b->b.field_146127_k==-31000&&b.field_146124_l))press(gui,-31000);stage++;return;}
  if(stage==6){check(tile.enchantments.contains(chosen),"new selection works after reload");
   check(ids(gui).size()==16,"start button tested with two full icon rows");
   GuiButton start=buttons(gui).stream().filter(b->b.field_146127_k==0).findFirst().get();
   check(buttons(gui).stream().filter(b->b.field_146127_k>=1&&b.field_146127_k<=16&&b.field_146124_l).noneMatch(b->start.field_146128_h<b.field_146128_h+b.field_146120_f&&start.field_146128_h+start.field_146120_f>b.field_146128_h&&start.field_146129_i<b.field_146129_i+b.field_146121_g&&start.field_146129_i+start.field_146121_g>b.field_146129_i),"start button clear of every enchantment hitbox");
   shot(mc,"osmotic-selected.png");mc.field_71439_g.func_71165_d("/tdosmotic empty");stage++;return;}
  if(stage==7){GuiButton start=buttons(gui).stream().filter(b->b.field_146127_k==0).findFirst().get();check(!start.field_146124_l,"start disabled when selected cost exceeds wand vis");start.func_146112_a(mc,start.field_146128_h+2,start.field_146129_i+2);check(gui.tooltip.stream().anyMatch(line->line.contains("missing 6 vis")),"tooltip shows actual missing vis");gui.tooltip.clear();ThaumicTinkerer.netHandler.sendToServer(new PacketEnchanterStartWorking(tile));stage++;return;}
  if(stage==8){check(!tile.working&&tile.currentAspects.size()==0,"server rejects unaffordable start packet");mc.field_71439_g.func_71165_d("/tdosmotic fill");stage++;return;}
  if(stage==9){press(gui,0);stage++;return;}
  if(stage==10){check(!tile.working&&net.minecraft.enchantment.EnchantmentHelper.func_77506_a(chosen,tile.func_70301_a(0))==1,"actual enchanting completion synchronized");ItemStack wand=tile.func_70301_a(1);check(((thaumcraft.common.items.wands.ItemWandCasting)wand.func_77973_b()).getVis(wand,thaumcraft.api.aspects.Aspect.AIR)==4400,"native wand payment synchronized");check(ids(gui).isEmpty(),"enchanted item has no eligible additions");System.out.println("TD_OSMOTIC_REMOTE_PASS checks="+checks);done=true;mc.field_71439_g.func_71165_d("/stop");mc.func_71400_g();}
 }catch(Throwable t){done=true;System.out.println("TD_OSMOTIC_REMOTE_FAILED checks="+checks);t.printStackTrace();if(mc.field_71439_g!=null)mc.field_71439_g.func_71165_d("/stop");mc.func_71400_g();}}
}
