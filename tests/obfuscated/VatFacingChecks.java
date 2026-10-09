package tdtest;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.world.WorldServer;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import thaumcraft.common.config.ConfigBlocks;
import com.mojang.authlib.GameProfile;
import com.kentington.thaumichorizons.common.tiles.TileVat;
import org.fentanylsolutions.thaumicdabblery.feature.vatfacing.*;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.VatZen;
import minetweaker.MineTweakerImplementationAPI;
public final class VatFacingChecks {
 public int checks;
 public void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
 public static final String DEMO="import mods.thaumichorizons.Vat;\nVat.setRotationItem(<minecraft:diamond>);\nVat.setTracking(\"Pig\",\"none\");\nVat.setTracking(\"effigy\",\"head\",16,60,30,6);\nVat.setTracking(\"Zombie\",\"body\",16,60,30,6);\n";
 public static void script(String text)throws Exception{Files.createDirectories(Paths.get("scripts"));Files.write(Paths.get("scripts/zz-vat-facing.zs"),text.getBytes(StandardCharsets.UTF_8));MineTweakerImplementationAPI.reload();}
 public static TileVat assembled(WorldServer w,int x,int y,int z)throws Exception{
  for(int dy=0;dy<4;dy++)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
   boolean center=dx==0&&dz==0,cap=dy==0||dy==3;
   w.func_147465_d(x+dx,y-dy,z+dz,cap?(center?ConfigBlocks.blockMetalDevice:ConfigBlocks.blockWoodenDevice):(center?Blocks.field_150355_j:Blocks.field_150359_w),cap?(center?9:6):0,3);
  }
  Class<?> cls=Class.forName("com.kentington.thaumichorizons.common.items.WandManagerTH");java.lang.reflect.Method replace=cls.getDeclaredMethod("replaceVat",net.minecraft.world.World.class,int.class,int.class,int.class);replace.setAccessible(true);replace.invoke(cls.newInstance(),w,x-1,y-3,z-1);return (TileVat)w.func_147438_o(x,y,z);
 }
 public static EntityItemFrame frame(WorldServer w,int x,int y,int z,int side,ItemStack item){EntityItemFrame f=new EntityItemFrame(w,x,y,z,side);if(!f.func_70518_d())throw new AssertionError("invalid frame surface");f.func_82334_a(item);w.func_72838_d(f);return f;}
 private void ticks(TileVat vat,int n){for(int i=0;i<n;i++)VatFacing.tick(vat);}
 private void invalid(Runnable operation){try{operation.run();throw new AssertionError("accepted invalid setting");}catch(IllegalArgumentException expected){checks++;}}
 public void run(WorldServer w)throws Exception{
  check(Boolean.FALSE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment")),"obfuscated runtime");
  script("");ItemStack original=VatFacing.rotationItem;check(original!=null,"default brain available");
  script(DEMO);check(VatFacing.rotationItem.func_77973_b()==Items.field_151045_i,"diamond script compiled");check(VatFacing.RULES.size()==3,"three scripted rules");
  TileVat vat=assembled(w,40,100,40);EntityPig pig=new EntityPig(w);vat.setEntityContained(pig);
  EntityItemFrame north=frame(w,40,99,39,2,new ItemStack(Items.field_151045_i));VatFacing.State state=VatFacing.state(vat);
  for(int rotation=0;rotation<4;rotation++){north.func_82336_g(rotation);ticks(vat,65);check(state.active&&Math.abs(net.minecraft.util.MathHelper.func_76142_g(state.body-(180+90*rotation)))<0.01,"frame selects quarter turn "+rotation);}
  check(pig.field_70177_z==0&&pig.field_70761_aq==0,"visual facing does not mutate subject");
  EntityItemFrame west=frame(w,39,99,40,1,new ItemStack(Items.field_151045_i));ticks(vat,65);check(state.frame.equals(west.func_110124_au()),"new eligible frame becomes active");
  north.func_82336_g(0);ticks(vat,65);check(state.frame.equals(north.func_110124_au()),"last rotated frame wins");
  NBTTagCompound saved=new NBTTagCompound();vat.func_145841_b(saved);TileVat restored=new TileVat();restored.func_145839_a(saved);check(VatFacing.state(restored).frame.equals(north.func_110124_au()),"frame selection saved");
  north.func_82334_a(null);ticks(vat,65);check(state.frame.equals(west.func_110124_au()),"removed item falls back to remaining control");
  west.func_82334_a(new ItemStack(Items.field_151043_k));ticks(vat,3);check(!state.active,"unrecognized item releases manual control");
  EntityPlayerMP target=new EntityPlayerMP(net.minecraft.server.MinecraftServer.func_71276_C(),w,new GameProfile(UUID.randomUUID(),"FacingTarget"),new net.minecraft.server.management.ItemInWorldManager(w));w.field_73010_i.add(target);
  try{
   target.func_70107_b(43,99,40.5);vat.setEntityContained(new EntityZombie(w));ticks(vat,65);check(state.target==target.func_145782_y()&&Math.abs(state.body+90)<0.01,"whole-body tracking faces nearest player");check(Math.abs(state.head)<0.01,"body tracking head stays aligned");
   north.func_82334_a(new ItemStack(Items.field_151045_i));north.func_82336_g(0);ticks(vat,65);check(Math.abs(Math.abs(state.body)-180)<0.01&&Math.abs(state.head)<0.01,"frame overrides body tracking");
   VatZen.setTracking("Zombie","head");ticks(vat,65);check(Math.abs(Math.abs(state.body)-180)<0.01&&Math.abs(state.head)>20&&Math.abs(state.head)<=60,"head tracking coexists with fixed frame facing");
   north.func_82334_a(null);VatZen.setTracking("Zombie","body");ticks(vat,65);check(Math.abs(state.body+90)<0.01,"removing frame restores body tracking");
   check(VatFacing.approach(179,-179,6)==-179,"shortest turn across angle boundary");
   vat.setEntityContained(null);vat.mode=4;target.func_70107_b(40.5,99,45);ticks(vat,65);check(Math.abs(Math.abs(state.body)-180)<0.01,"effigy head mode preserves native body direction");check(Math.abs(state.head)<=60&&Math.abs(state.pitch)<=30,"head and pitch clamped for player behind");
   float before=state.head;target.func_70107_b(35,99,40);VatFacing.tick(vat);check(Math.abs(state.head-before)<=6.01,"turn speed bounded");
   for(int i=0;i<360;i++){double a=Math.toRadians(i);target.func_70107_b(40.5+5*Math.sin(a),99,40.5+5*Math.cos(a));VatFacing.tick(vat);check(Math.abs(state.head)<=60.01&&Math.abs(state.pitch)<=30.01,"bounded head through full circle");}
   target.func_70107_b(140,99,40);ticks(vat,65);check(state.target==-1&&Math.abs(state.head)<0.01&&Math.abs(state.pitch)<0.01,"out of range returns neutral");
   script(DEMO.replace("\"head\",16,60,30,6","\"head\",16,20,10,3"));target.func_70107_b(44,104,40);ticks(vat,65);check(Math.abs(state.head)<=20&&Math.abs(state.pitch)<=10&&state.speed==3,"custom limits reload");
   invalid(()->VatZen.setTracking("MissingMob","head"));invalid(()->VatZen.setTracking("Pig","invalid"));invalid(()->VatZen.setTracking("Pig","head",8,180,30,6));invalid(()->VatZen.setTracking("Pig","head",Double.NaN,60,30,6));invalid(()->VatZen.setTracking("effigy","body",8,60,30,0));
   script("");ticks(vat,3);check(!state.active&&VatFacing.RULES.isEmpty()&&VatFacing.rotationItem.func_77973_b()==original.func_77973_b(),"script removal restores defaults and releases pose");
  }finally{w.field_73010_i.remove(target);vat.mode=0;script(DEMO);}
 }
}
