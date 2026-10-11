package tdtest;
import java.util.*;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import com.kentington.thaumichorizons.common.tiles.TileVat;
import org.fentanylsolutions.thaumicdabblery.feature.effigyskins.*;
@Mod(modid="tdrevivalclient",name="Effigy revival client probe",version="1",acceptableRemoteVersions="*",dependencies="required-after:thaumicdabblery;required-after:ThaumicHorizons")
public final class EffigyRevivalClientProbe {
 private boolean bubbleShot,sparkleShot;
 private int ticks,stage,wait,checks;private boolean launched,done;private final Set<Object> bubbles=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>()),sparkles=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
 private void check(boolean ok,String what){checks++;if(!ok)throw new AssertionError(what);}
 @Mod.EventHandler public void ready(FMLLoadCompleteEvent e){FMLCommonHandler.instance().bus().register(this);net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);}
 @SubscribeEvent public void frame(net.minecraftforge.client.event.RenderWorldLastEvent e){
  Minecraft mc=Minecraft.func_71410_x();String name=null;
  if(!bubbleShot&&bubbles.size()>=20){bubbleShot=true;name="revival-bubbles.png";}
  if(!sparkleShot&&!sparkles.isEmpty()){sparkleShot=true;name="revival-sparkles.png";}
  if(name!=null)net.minecraft.util.ScreenShotHelper.func_148259_a(new java.io.File("."),name,mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());
 }
 private void particles(Minecraft mc)throws Exception{
  java.lang.reflect.Field field=thaumcraft.client.fx.ParticleEngine.class.getDeclaredField("particles");field.setAccessible(true);
  for(Object map:(Object[])field.get(thaumcraft.client.fx.ParticleEngine.instance)){
   Object list=((Map)map).get(mc.field_71441_e.field_73011_w.field_76574_g);if(list==null)continue;
   for(Object particle:((List)list).toArray()){
    if(particle==null)continue;net.minecraft.client.particle.EntityFX fx=(net.minecraft.client.particle.EntityFX)particle;
    if(Math.abs(fx.field_70165_t-.5)>2||Math.abs(fx.field_70161_v-.5)>2||fx.field_70163_u<4||fx.field_70163_u>8)continue;
    if(particle.getClass().getSimpleName().equals("FXEssentiaBubble"))bubbles.add(particle);
    if(particle.getClass().getSimpleName().equals("FXGeneric"))sparkles.add(particle);
   }
  }
 }
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END||done||++ticks<30)return;Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;mc.field_71474_y.field_74362_aa=0;
  try{
   if(!launched){launched=true;cpw.mods.fml.client.FMLClientHandler.instance().connectToServerAtStartup("127.0.0.1",Integer.getInteger("td.vat.port",25731));return;}
   if(mc.field_71462_r instanceof net.minecraft.client.gui.GuiDisconnected)throw new AssertionError("Disconnected");if(ticks>1400)throw new AssertionError("Timeout stage="+stage+" bubbles="+bubbles.size()+" sparkles="+sparkles.size());
   if(mc.field_71439_g==null||mc.field_71441_e==null||!(mc.field_71441_e.func_147438_o(0,7,0) instanceof TileVat))return;
   TileVat vat=(TileVat)mc.field_71441_e.func_147438_o(0,7,0);particles(mc);
   if(stage==0){if(EffigySkinClient.profileFor(vat)==null)return;check(!mc.func_71387_A(),"separate dedicated server");mc.field_71439_g.func_71165_d("/tdrevival start");stage=1;wait=0;return;}
   if(stage==1){if(vat.mode!=3)return;if(++wait<40)return;check(bubbles.isEmpty()&&sparkles.isEmpty(),"no intake or completion particles while starved");check(!EffigySkinClient.render(mc.field_71441_e.func_147438_o(0,6,0),.1F),"bound unrevived effigy uses native texture");mc.field_71439_g.func_71165_d("/tdrevival feed");stage=2;wait=0;return;}
   if(stage==2){if(vat.mode!=4)return;if(++wait<35)return;System.out.println("TD_REVIVAL_PARTICLES bubbles="+bubbles.size()+" sparkles="+sparkles.size());check(bubbles.size()==160,"ten native bubbles per unit of actual jar intake");check(sparkles.size()==2*thaumcraft.common.Thaumcraft.proxy.particleCount(20),"one completion burst at each effigy block");check(EffigySkinClient.profileFor(vat).getId().equals(mc.field_71439_g.func_146103_bH().getId()),"revived bound identity retained");mc.field_71439_g.func_71165_d("/tdrevival verify");stage=3;wait=0;return;}
   if(stage==3&&++wait>35){check(sparkles.size()==2*thaumcraft.common.Thaumcraft.proxy.particleCount(20)&&bubbles.size()==160,"completed vat does not repeat effects");System.out.println("TD_REVIVAL_CLIENT_PASS checks="+checks);done=true;mc.field_71439_g.func_71165_d("/stop");mc.func_71400_g();}
  }catch(Throwable t){done=true;System.out.println("TD_REVIVAL_CLIENT_FAILED checks="+checks);t.printStackTrace();if(mc.field_71439_g!=null)mc.field_71439_g.func_71165_d("/stop");mc.func_71400_g();}
 }
}
