package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import org.fentanylsolutions.thaumicdabblery.feature.vatfacing.*;
import org.fentanylsolutions.thaumicdabblery.feature.effigyskins.*;
import com.kentington.thaumichorizons.common.tiles.TileVat;
@Mod(modid="tdvatappearanceremoteprobe",name="Remote vat appearance probe",version="1",acceptableRemoteVersions="*",dependencies="required-after:thaumicdabblery;required-after:ThaumicHorizons;after:tc4tweak;after:salisarcana")
public final class VatAppearanceRemoteProbe {
 private int ticks,stage,wait,checks,rotations,initialRotation;private boolean launched,done;private EntityItemFrame frame;
 private void check(boolean c,String message){checks++;if(!c)throw new AssertionError(message);}
 @Mod.EventHandler public void ready(FMLLoadCompleteEvent e){FMLCommonHandler.instance().bus().register(this);}
 private float[] matrix(){java.nio.FloatBuffer b=org.lwjgl.BufferUtils.createFloatBuffer(16);org.lwjgl.opengl.GL11.glGetFloat(org.lwjgl.opengl.GL11.GL_MODELVIEW_MATRIX,b);float[] values=new float[16];b.get(values);return values;}
 private void near(double a,double b,String name){check(Math.abs(a-b)<.0001,name+" "+a+" != "+b);}
 private void appearance(Minecraft mc,TileVat pig,TileVat effigy,TileVat zombie){
  VatFacing.State p=VatFacing.state(pig),e=VatFacing.state(effigy),z=VatFacing.state(zombie);
  check(p.customBobbing&&p.bobPeriod==120,"server per-entity bob reaches remote client");near(p.bobAmplitude,.4,"amplitude sync");near(p.yOffset,.3,"Y sync");near(p.scale,1.5,"scale sync");check(z.customBobbing&&z.bobPeriod==80,"global bob reaches other species");check(e.customBobbing&&e.bobAmplitude==0,"effigy bob disabled");near(e.scale,.8,"effigy scale sync");near(e.yOffset,.2,"effigy Y sync");
  VatAppearance.globalBobbing=null;VatAppearance.BOBBING.clear();VatAppearance.SCALES.clear();VatAppearance.Y_OFFSETS.clear();
  TileEntity interior=mc.field_71441_e.func_147438_o(-7,6,0),effigyInterior=mc.field_71441_e.func_147438_o(0,6,0);
  float original=.1F*(float)Math.cos(Math.toRadians(mc.field_71439_g.field_70173_aa));
  near(VatFacingClient.bobbingY(interior,original,.5F),VatAppearance.bob(p,mc.field_71441_e.func_82737_E(),.5F),"native pig wave replaced");
  near(VatFacingClient.bobbingY(effigyInterior,2+original,.5F),2,"native effigy wave stopped");
  EntityLivingBase mob=pig.getEntityContained();double oldY=mob.field_70163_u;float width=mob.field_70130_N;
  org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);org.lwjgl.opengl.GL11.glPushMatrix();
  try{org.lwjgl.opengl.GL11.glLoadIdentity();VatFacingClient.render(interior,mob,.5F,()->{
   float[] m=matrix();near(m[0],1.5,"X scale in draw");near(m[5],1.5,"Y scale in draw");near(m[10],1.5,"Z scale in draw");
   double origin=mob.field_70137_T+(mob.field_70163_u-mob.field_70137_T)*.5-net.minecraft.client.renderer.entity.RenderManager.field_78726_c;
   near(m[13]+.5*origin,.3,"scale pivot and world Y offset");return true;});
   near(matrix()[0],1,"matrix restored after draw");near(matrix()[13],0,"translation restored after draw");
   try{VatFacingClient.render(interior,mob,.5F,()->{throw new IllegalStateException("test draw failure");});throw new AssertionError("no failure");}catch(IllegalStateException expected){}
   near(matrix()[0],1,"matrix restored after exception");near(matrix()[13],0,"translation restored after exception");
  }finally{org.lwjgl.opengl.GL11.glPopMatrix();}
  near(mob.field_70163_u,oldY,"physical Y unchanged");near(mob.field_70130_N,width,"hitbox unchanged");
 }
 private TileVat vat(Minecraft mc,int x){TileEntity tile=mc.field_71441_e.func_147438_o(x,7,0);return tile instanceof TileVat?(TileVat)tile:null;}
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END||done||++ticks<30)return;Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;
  try{
   if(!launched){launched=true;cpw.mods.fml.client.FMLClientHandler.instance().connectToServerAtStartup("127.0.0.1",Integer.getInteger("td.vat.port",25569));return;}
   if(mc.field_71462_r instanceof net.minecraft.client.gui.GuiDisconnected){for(java.lang.reflect.Field f:mc.field_71462_r.getClass().getDeclaredFields())if(net.minecraft.util.IChatComponent.class.isAssignableFrom(f.getType())){f.setAccessible(true);System.out.println("TD_VAT_DISCONNECT "+f.get(mc.field_71462_r));}throw new AssertionError("Disconnected during remote test");}
   if(ticks>1200)throw new AssertionError("timeout stage="+stage);if(mc.field_71439_g==null||mc.field_71441_e==null)return;
   TileVat pig=vat(mc,-7),effigy=vat(mc,0),zombie=vat(mc,7);if(pig==null||effigy==null||zombie==null)return;
   if(stage==0){
    if(!VatFacing.state(pig).active||!VatFacing.state(effigy).active||!VatFacing.state(zombie).active)return;
    appearance(mc,pig,effigy,zombie);check(!mc.func_71356_B(),"real dedicated server connection");check(EffigySkinClient.profileFor(effigy).getId().equals(mc.field_71439_g.func_146103_bH().getId()),"effigy bound to connected player");
    for(Object o:mc.field_71441_e.field_72996_f)if(o instanceof EntityItemFrame&&((EntityItemFrame)o).field_146063_b==-8)frame=(EntityItemFrame)o;
    check(frame!=null,"control frame synchronized");initialRotation=frame.func_82333_j();mc.field_71439_g.func_71165_d("/tp Developer -8 4 -3");stage=1;wait=0;return;
   }
   if(++wait<50)return;
   if(stage==1){
    check(Math.abs(net.minecraft.util.MathHelper.func_76142_g(VatFacing.state(pig).body-(180+(rotations+initialRotation)*90)))<1,"remote frame facing "+rotations+" actual="+VatFacing.state(pig).body+" goal="+VatFacing.state(pig).goalBody+" frame="+frame.func_82333_j());
    if(rotations++<3){mc.field_71442_b.func_78768_b(mc.field_71439_g,frame);wait=0;return;}
    mc.field_71439_g.func_71165_d("/tp Developer 12 4 -3");stage=2;wait=0;return;
   }
   if(stage==2){
    VatFacing.State z=VatFacing.state(zombie),f=VatFacing.state(effigy);
    double dx=mc.field_71439_g.field_70165_t-7.5,dz=mc.field_71439_g.field_70161_v-0.5;float yaw=(float)Math.toDegrees(Math.atan2(dz,dx))-90;
    check(Math.abs(net.minecraft.util.MathHelper.func_76142_g(z.body-yaw))<2,"remote body tracks actual player position");check(Math.abs(f.head)>20&&Math.abs(f.head)<=60&&Math.abs(f.body)==180,"remote effigy moves only its bounded head");
    EntityLivingBase mob=zombie.getEntityContained();float original=mob.field_70761_aq;
    check(VatFacingClient.render(mc.field_71441_e.func_147438_o(7,6,0),mob,1,()->{check(Math.abs(net.minecraft.util.MathHelper.func_76142_g(mob.field_70761_aq-z.body))<0.01,"renderer receives facing");return true;}),"draw callback");check(mob.field_70761_aq==original,"render restores stored creature orientation");
    mc.field_71439_g.func_71165_d("/tp Developer 0.5 4 -8");stage=3;wait=0;return;
   }
   if(stage==3){net.minecraft.util.ScreenShotHelper.func_148259_a(new java.io.File("."),"vat-appearance-demo.png",mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());System.out.println("TD_VAT_APPEARANCE_REMOTE_PASS checks="+checks);done=true;mc.field_71439_g.func_71165_d("/stop");mc.func_71400_g();}
  }catch(Throwable t){done=true;System.out.println("TD_VAT_APPEARANCE_REMOTE_FAILED checks="+checks);t.printStackTrace();if(mc.field_71439_g!=null)mc.field_71439_g.func_71165_d("/stop");mc.func_71400_g();}
 }
}
