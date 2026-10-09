package tdtest;
import net.minecraft.world.WorldServer;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.nbt.NBTTagCompound;
import com.kentington.thaumichorizons.common.tiles.TileVat;
import org.fentanylsolutions.thaumicdabblery.feature.vatfacing.*;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.VatZen;
public final class VatAppearanceChecks {
 public int checks;
 public static final String SCRIPT="import mods.thaumichorizons.Vat;\nVat.setBobbing(0.2,80);\nVat.setBobbing(\"Pig\",0.4,120);\nVat.setBobbing(\"effigy\",0,80);\nVat.setYOffset(\"Pig\",0.3);\nVat.setScale(\"Pig\",1.5);\nVat.setYOffset(\"effigy\",0.2);\nVat.setScale(\"effigy\",0.8);\n";
 private void check(boolean c,String text){checks++;if(!c)throw new AssertionError(text);}
 private void near(double a,double b,String text){check(Math.abs(a-b)<0.00001,text+" "+a+" != "+b);}
 private void invalid(Runnable r){try{r.run();throw new AssertionError("invalid value accepted");}catch(IllegalArgumentException expected){checks++;}}
 public void run(WorldServer w)throws Exception{
  VatFacingChecks.script("");TileVat vat=VatFacingChecks.assembled(w,60,100,60);EntityPig pig=new EntityPig(w);vat.setEntityContained(pig);VatFacing.tick(vat);VatFacing.State s=VatFacing.state(vat);
  check(!s.customBobbing&&s.scale==1&&s.yOffset==0,"native defaults");
  VatFacingChecks.script(SCRIPT);VatFacing.tick(vat);check(s.customBobbing&&s.bobPeriod==120,"per creature bob script");near(s.bobAmplitude,.4,"amplitude");near(s.yOffset,.3,"offset");near(s.scale,1.5,"scale");check(!s.active,"appearance independent of facing");
  near(VatAppearance.bob(s,0,0),.4,"crest");near(VatAppearance.bob(s,30,0),0,"quarter");near(VatAppearance.bob(s,60,0),-.4,"trough");near(VatAppearance.bob(s,120,0),.4,"wrap");near(VatAppearance.bob(s,Long.MAX_VALUE-7,0),VatAppearance.bob(s,(Long.MAX_VALUE-7)%120,0),"long world clock");check(VatAppearance.bob(s,20,.5F)!=VatAppearance.bob(s,20,0),"partial ticks smooth animation");
  NBTTagCompound data=new NBTTagCompound();s.write(data);VatFacing.State client=new VatFacing.State();client.read(data);near(client.scale,1.5,"scale packet");near(client.yOffset,.3,"offset packet");check(client.customBobbing&&client.bobPeriod==120,"bob packet");near(client.bobAmplitude,.4,"amplitude packet");
  NBTTagCompound disk=new NBTTagCompound();vat.func_145841_b(disk);TileVat restored=new TileVat();restored.func_145839_a(disk);near(VatFacing.state(restored).scale,1.5,"disk save");
  VatZen.setBobbing(.1,200);VatFacing.tick(vat);check(s.bobPeriod==120,"override beats later global");vat.setEntityContained(new EntityZombie(w));VatFacing.tick(vat);check(s.bobPeriod==200&&s.yOffset==0&&s.scale==1,"global and species switch");
  vat.setEntityContained(null);vat.mode=4;VatFacing.tick(vat);check(s.customBobbing&&s.bobAmplitude==0,"zero stops native bob");near(s.yOffset,.2,"effigy offset");near(s.scale,.8,"effigy scale");vat.mode=0;VatFacing.tick(vat);check(!s.customBobbing&&s.yOffset==0&&s.scale==1,"empty vat reset");
  for(int i=0;i<3;i++){VatFacingChecks.script(SCRIPT);check(VatAppearance.BOBBING.size()==2&&VatAppearance.SCALES.size()==2,"reload stable");}
  invalid(()->VatZen.setBobbing(-1,80));invalid(()->VatZen.setBobbing(Double.NaN,80));invalid(()->VatZen.setBobbing(.1,0));invalid(()->VatZen.setBobbing(.1,72001));invalid(()->VatZen.setBobbing("MissingMob",.1,80));invalid(()->VatZen.setScale("Pig",0));invalid(()->VatZen.setScale("Pig",Double.POSITIVE_INFINITY));invalid(()->VatZen.setYOffset("Pig",Double.NaN));invalid(()->VatZen.setYOffset("Pig",5));
  VatFacingChecks.script("");check(VatAppearance.globalBobbing==null&&VatAppearance.BOBBING.isEmpty()&&VatAppearance.SCALES.isEmpty()&&VatAppearance.Y_OFFSETS.isEmpty(),"removal restores globals and all overrides");
  client.read(new NBTTagCompound());check(client.scale==1&&!client.customBobbing&&client.yOffset==0,"old saves default correctly");
  vat.setEntityContained(pig);double y=pig.field_70163_u;VatFacing.tick(vat);near(pig.field_70163_u,y,"no physical repositioning");near(pig.field_70130_N,.9,"no physical resize");
 }
}
