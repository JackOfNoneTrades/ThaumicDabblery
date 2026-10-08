package tdtest;
import java.util.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.kentington.thaumichorizons.common.ThaumicHorizons;
import com.kentington.thaumichorizons.common.tiles.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.nbt.*;
import net.minecraft.world.WorldServer;
import org.fentanylsolutions.thaumicdabblery.feature.effigyskins.*;
import io.netty.buffer.Unpooled;

public final class EffigySkinChecks {
 public int checks;
 public void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
 public static NBTTagCompound data(EntityPlayerMP p)throws Exception{return (NBTTagCompound)p.getClass().getMethod("getEntityData").invoke(p);}
 public static GameProfile profile(Object tile){return ((EffigySkinHolder)tile).thaumicdabblery$getEffigyProfile();}
 public static void profile(Object tile,GameProfile profile){((EffigySkinHolder)tile).thaumicdabblery$setEffigyProfile(profile);}
 public static TileVat vat(WorldServer w,int x,int y,int z){w.func_147465_d(x,y,z,ThaumicHorizons.blockVatSolid,7,3);TileVat v=(TileVat)w.func_147438_o(x,y,z);v.mode=4;return v;}
 public static TileSoulBeacon beacon(WorldServer w,int x,int y,int z){w.func_147465_d(x,y,z,ThaumicHorizons.blockSoulBeacon,0,3);return (TileSoulBeacon)w.func_147438_o(x,y,z);}
 public static EntityPlayerMP player(WorldServer w,GameProfile p){return new EntityPlayerMP(net.minecraft.server.MinecraftServer.func_71276_C(),w,p,new net.minecraft.server.management.ItemInWorldManager(w)){@Override public void func_145747_a(net.minecraft.util.IChatComponent message){}};}
 public void run(WorldServer w)throws Exception{
  check(Boolean.FALSE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment")),"obfuscated runtime");
  TileVat vat=vat(w,40,100,0);TileSoulBeacon beacon=beacon(w,40,101,0);
  GameProfile a=new GameProfile(UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),"EffigyAlice"),b=new GameProfile(UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),"EffigyBob");
  a.getProperties().put("textures",new Property("textures","saved-skin-a","saved-signature-a"));
  EntityPlayerMP alice=player(w,a),bob=player(w,b);
  beacon.activate(alice);EffigySkinNetwork.Binding bindingA=EffigySkinNetwork.Binding.from(data(alice));
  check(bindingA.matches(vat)&&Arrays.equals(data(alice).func_74759_k("soulBeaconCoords"),new int[]{40,101,0}),"native binding unchanged");
  check(profile(vat).getId().equals(a.getId())&&profile(beacon).getId().equals(a.getId()),"beacon and vat record binding player");
  check(profile(vat)!=alice.func_146103_bH()&&profile(vat).getProperties().get("textures").iterator().next().getValue().equals("saved-skin-a"),"profile copied with texture property");
  beacon.activate(bob);EffigySkinNetwork.Binding bindingB=EffigySkinNetwork.Binding.from(data(bob));
  check(EffigySkins.select(vat,a,bindingA).getId().equals(a.getId()),"Alice sees herself on shared beacon");
  check(EffigySkins.select(vat,b,bindingB).getId().equals(b.getId()),"Bob sees himself on shared beacon");
  check(EffigySkins.select(vat,a,new EffigySkinNetwork.Binding()).getId().equals(b.getId()),"unbound observer sees latest binder");
  check(EffigySkins.select(vat,null,null).getId().equals(b.getId()),"no client player safely selects saved appearance");
  TileSoulBeacon elsewhere=beacon(w,44,101,0);elsewhere.activate(alice);
  check(EffigySkins.select(vat,a,EffigySkinNetwork.Binding.from(data(alice))).getId().equals(b.getId()),"rebinding elsewhere stops personal appearance at old beacon");
  check(bindingB.matches(vat),"other player remains bound to original");
  NBTTagCompound nativeData=new NBTTagCompound();nativeData.func_74757_a("soulBeacon",true);nativeData.func_74783_a("soulBeaconCoords",new int[]{40,101,0});nativeData.func_74768_a("soulBeaconDim",w.field_73011_w.field_76574_g+1);
  check(!EffigySkinNetwork.Binding.from(nativeData).matches(vat),"dimension is part of cosmetic binding");
  nativeData.func_74783_a("soulBeaconCoords",new int[]{40});check(!EffigySkinNetwork.Binding.from(nativeData).matches(vat),"malformed binding coordinates are ignored");
  io.netty.buffer.ByteBuf bytes=Unpooled.buffer();bindingB.toBytes(bytes);check(bytes.readableBytes()==17,"small bound-player packet");EffigySkinNetwork.Binding decoded=new EffigySkinNetwork.Binding();decoded.fromBytes(bytes);check(decoded.matches(vat),"network binding roundtrip");bytes.release();
  bytes=Unpooled.buffer();new EffigySkinNetwork.Binding().toBytes(bytes);check(bytes.readableBytes()==1,"unbound packet clears previous binding");decoded.fromBytes(bytes);check(!decoded.matches(vat),"unbound packet roundtrip");bytes.release();
  beacon.activate(alice);
  NBTTagCompound saved=new NBTTagCompound();vat.func_145841_b(saved);TileVat restored=new TileVat();restored.func_145839_a(saved);
  check(profile(restored).getId().equals(a.getId())&&profile(restored).getProperties().get("textures").iterator().next().getSignature().equals("saved-signature-a"),"vat disk save retains UUID and signed texture property");
  NBTTagCompound packet=new NBTTagCompound();vat.writeCustomNBT(packet);TileVat client=new TileVat();client.readCustomNBT(packet);check(profile(client).getId().equals(a.getId()),"vat update NBT carries identity");
  saved=new NBTTagCompound();beacon.func_145841_b(saved);TileSoulBeacon restoredBeacon=new TileSoulBeacon();restoredBeacon.func_145839_a(saved);check(profile(restoredBeacon).getId().equals(a.getId()),"beacon save retains identity");
  packet=new NBTTagCompound();beacon.writeCustomNBT(packet);restoredBeacon=new TileSoulBeacon();restoredBeacon.readCustomNBT(packet);check(profile(restoredBeacon).getId().equals(a.getId()),"beacon update NBT carries identity");
  client.readCustomNBT(new NBTTagCompound());check(profile(client)==null,"old save or empty sync clears appearance");
  NBTTagCompound malformed=new NBTTagCompound(),badProfile=new NBTTagCompound();badProfile.func_74778_a("Id","invalid");malformed.func_74782_a("thaumicdabblery:effigySkin",badProfile);client.readCustomNBT(malformed);check(profile(client)==null,"invalid saved identity falls back without crash");
  vat.setEntityContained(new EntityPig(w));check(profile(vat)==null,"occupied or consumed body clears appearance");vat.setEntityContained(null);vat.mode=4;vat.func_145845_h();check(profile(vat).getId().equals(a.getId()),"later effigy inherits bound beacon profile");
  w.func_147468_f(40,101,0);vat.recipeType=1;vat.mode=2;check(profile(vat).getId().equals(a.getId()),"removing beacon for self-infusion retains appearance");
  vat.mode=0;vat.recipeType=0;vat.killSubject();check(profile(vat)==null,"destroyed subject clears appearance");
  TileVat loose=new TileVat();loose.func_145834_a(w);loose.field_145851_c=60;loose.field_145848_d=100;profile(loose,a);loose.killMe();check(profile(loose)==null,"dismantled vat clears appearance");
  TileSoulBeacon early=beacon(w,70,101,0);early.activate(bob);TileVat later=vat(w,70,100,0);later.func_145845_h();check(profile(later).getId().equals(b.getId()),"binding before vat construction is inherited");
  later.mode=0;
 }
}
