package tdtest;

import java.util.*;
import java.nio.file.*;
import java.lang.reflect.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.command.*;
import net.minecraft.entity.*;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.*;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.network.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.util.*;
import net.minecraft.nbt.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import thaumcraft.api.aspects.*;
import thaumcraft.api.research.*;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.research.*;
import thaumcraft.common.lib.network.playerdata.*;
import org.fentanylsolutions.thaumicdabblery.feature.scanall.*;
import org.fentanylsolutions.thaumicdabblery.feature.researchscangates.ScanGateRegistry;
import io.netty.buffer.*;

public final class ScanAllChecks {
 public int checks;
 public void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static ItemStack stack(String name,int meta){return new ItemStack((Item)Item.field_150901_e.func_82594_a("minecraft:"+name),1,meta);}
 public static void setup(){
  boolean standalone=!cpw.mods.fml.common.Loader.isModLoaded("MineTweaker3");
  if(standalone){
   Aspect a=new Aspect("tdhidden",0x8844ff,new Aspect[]{Aspect.AIR,Aspect.ORDER});
   thaumcraft.api.ThaumcraftApi.registerObjectTag(stack("clock",0),new AspectList().add(a,3));
  }
  ResearchCategories.registerCategory("TD_SCAN",new ResourceLocation("thaumcraft","textures/aspects/ordo.png"),new ResourceLocation("thaumcraft","textures/gui/gui_researchback.png"));
  for(String key:new String[]{"TD_SCAN_ITEM","TD_SCAN_ENTITY","TD_SCAN_ASPECT","TD_SCAN_GATE","TD_SCAN_UNRELATED"}){
   ResearchItem r=new ResearchItem(key,"TD_SCAN",new AspectList().add(Aspect.ORDER,1),0,0,1,stack("clock",0));r.setVirtual();r.setPages(new ResearchPage("Scan test"));
   if(!key.endsWith("UNRELATED"))r.setHidden();
   if(key.endsWith("ITEM"))r.setItemTriggers(stack("clock",0));
   if(key.endsWith("ENTITY"))r.setEntityTriggers("Cow");
   if(key.endsWith("ASPECT"))r.setAspectTriggers(Aspect.getAspect("tdhidden"));
   r.registerResearchItem();
  }
  if(standalone){ScanGateRegistry.requireItems("TD_SCAN_GATE",new ItemStack[]{stack("clock",0)});ScanGateRegistry.requireEntities("TD_SCAN_GATE",new String[]{"Cow"});}
 }
 public EntityPlayerMP fake(WorldServer world,String name){
  EntityPlayerMP p=FakePlayerFactory.get(world,new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()),name));
  p.field_71135_a=new NetHandlerPlayServer(MinecraftServer.func_71276_C(),new NetworkManager(false){private final io.netty.channel.Channel sink=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());public io.netty.channel.Channel channel(){return sink;}},p){@Override public void func_147359_a(Packet packet){}};
  return p;
 }
 public static ScanResult item(ItemStack s){return new ScanResult((byte)1,Item.func_150891_b(s.func_77973_b()),s.func_77960_j(),null,"");}
 public void verify(EntityPlayerMP p, Map<Aspect,Integer> before){
  String name=p.func_70005_c_();PlayerKnowledge k=Thaumcraft.proxy.getPlayerKnowledge();
  check(k.getAspectsDiscovered(name).size()==Aspect.aspects.size(),"all registered aspects discovered");
  for(Aspect a:Aspect.aspects.values())check(k.getAspectsDiscovered(name).getAmount(a)==(before.containsKey(a)?before.get(a):0),"points unchanged for "+a.getTag()+" expected="+before.get(a)+" actual="+k.getAspectsDiscovered(name).getAmount(a));
  for(String key:new String[]{"TD_SCAN_ITEM","TD_SCAN_ENTITY","TD_SCAN_ASPECT","TD_SCAN_GATE"}){
   check(ResearchManager.isResearchComplete(name,"@"+key),"scan clue revealed "+key);
   check(!ResearchManager.isResearchComplete(name,key),"normal research remains incomplete "+key);
  }
  check(!ResearchManager.isResearchComplete(name,"@TD_SCAN_UNRELATED"),"unrelated research remains unrevealed");
  check(ScanGateRegistry.hasRevealMarker(name,"TD_SCAN_GATE"),"Dabblery scan gate satisfied");
 }
 public void run(WorldServer world)throws Exception{
  check(Boolean.FALSE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment")),"obfuscated environment");
  EntityPlayerMP p=fake(world,"ScanAllProbe");String name=p.func_70005_c_();PlayerKnowledge k=Thaumcraft.proxy.getPlayerKnowledge();k.wipePlayerKnowledge(name);
  Aspect hidden=Aspect.getAspect("tdhidden");check(hidden!=null,"custom hidden primal registered");
  check(!k.hasDiscoveredAspect(name,hidden),"hidden primal initially unknown");
  k.addAspectPool(name,Aspect.AIR,(short)37);k.addAspectPool(name,Aspect.LIGHT,(short)11);
  Map<Aspect,Integer> before=new HashMap<>(k.getAspectsDiscovered(name).aspects);
  ResearchManager.completeScannedObjectUnsaved(name,"@12345");ResearchManager.completeScannedEntityUnsaved(name,"@67890");
  ScanAllCommand cmd=new ScanAllCommand();check(cmd.func_82362_a()==2,"operator permission level");
  try{cmd.func_71515_b(p,new String[]{"invalid"});throw new AssertionError("invalid usage accepted");}catch(WrongUsageException expected){check(true,"bad usage rejected");}
  check(MinecraftServer.func_71276_C().func_71187_D().func_71555_a().containsKey("td"),"server registered command alias");
  long start=System.nanoTime();cmd.func_71515_b(p,new String[]{"scanall"});System.out.println("TD_SCANALL_COMMAND_MS="+(System.nanoTime()-start)/1000000L);
  verify(p,before);
  check(k.objectsScanned.get(name) instanceof AllScanHistory && k.entitiesScanned.get(name) instanceof AllScanHistory,"both history lists wrapped");
  check(k.objectsScanned.get(name).size()==2 && k.entitiesScanned.get(name).size()==2,"constant-size markers preserve real history");
  check(k.objectsScanned.get(name).get(0).equals("@12345"),"original real scan preserved");
  check(!k.objectsScanned.get(name).contains("#NODE999") && !k.objectsScanned.get(name).contains("#bogus"),"wildcard only matches numeric scan identities");
  for(ItemStack s:new ItemStack[]{stack("clock",0),stack("wool",14),stack("wool",11),stack("diamond_sword",83)}){
   ScanResult scan=item(s);check(ScanManager.hasBeenScanned(p,scan),"item already scanned");
   check(!ScanManager.isValidScanTarget(p,scan,"@")&&!ScanManager.isValidScanTarget(p,scan,"#"),"both scan modes disabled");
   check(!ScanManager.completeScan(p,scan,"@"),"direct scan does not grant points");
   check(k.objectsScanned.get(name).contains("#"+ScanManager.generateItemHash(s.func_77973_b(),s.func_77960_j())),"native inventory-popup membership");
  }
  EntityCow calf=new EntityCow(world);calf.func_70873_a(-20000);
  EntityZombie zombie=new EntityZombie(world);zombie.func_82229_g(true);zombie.func_82227_f(true);
  EntityCreeper creeper=new EntityCreeper(world);creeper.func_70829_a(1);
  for(Entity e:new Entity[]{calf,zombie,creeper,p,new EntityItem(world,0,80,0,stack("clock",0))}){
   ScanResult scan=new ScanResult((byte)2,0,0,e,"");check(ScanManager.hasBeenScanned(p,scan),"entity variant scanned");
   check(!ScanManager.isValidScanTarget(p,scan,"#")&&!ScanManager.completeScan(p,scan,"#"),"entity cannot award scan rewards");
  }
  check(!ScanManager.hasBeenScanned(p,new ScanResult((byte)3,0,0,null,"NODE999")),"nodes excluded");
  EntityPlayerMP other=fake(world,"ScanAllOther");k.wipePlayerKnowledge(other.func_70005_c_());
  check(!ScanManager.hasBeenScanned(other,item(stack("clock",0))),"other player unaffected");
  check(!k.hasDiscoveredAspect(other.func_70005_c_(),hidden),"other player's hidden primal remains hidden");
  NBTTagCompound saved=new NBTTagCompound();ResearchManager.saveScannedNBT(saved,p);ResearchManager.saveAspectNBT(saved,p);ResearchManager.saveResearchNBT(saved,p);
  k.wipePlayerKnowledge(name);ResearchManager.loadAspectNBT(saved,p);ResearchManager.loadScannedNBT(saved,p);ResearchManager.loadResearchNBT(saved,p);
  verify(p,before);check(k.objectsScanned.get(name) instanceof AllScanHistory,"NBT restores wrapper");
  check(ScanManager.hasBeenScanned(p,item(stack("clock",0))),"NBT restores scan completion");
  Path disk=Paths.get("scanall-saved.dat");if(Files.exists(disk)){k.wipePlayerKnowledge(name);NBTTagCompound prior=CompressedStreamTools.func_74797_a(disk.toFile());ResearchManager.loadAspectNBT(prior,p);ResearchManager.loadScannedNBT(prior,p);ResearchManager.loadResearchNBT(prior,p);verify(p,before);System.out.println("TD_SCANALL_RESTART_PASS");}
  CompressedStreamTools.func_74795_b(saved,disk.toFile());
  ByteBuf bytes=Unpooled.buffer();new PacketSyncScannedItems(p).toBytes(bytes);check(bytes.readableBytes()<80,"compact item sync packet");
  PacketSyncScannedItems decoded=new PacketSyncScannedItems();decoded.fromBytes(bytes);check(bytes.readableBytes()==0,"packet consumes all bytes");bytes.release();
  Field data=PacketSyncScannedItems.class.getDeclaredField("data");data.setAccessible(true);
  k.objectsScanned.remove(name);for(String key:(ArrayList<String>)data.get(decoded))ResearchManager.completeScannedObjectUnsaved(name,key);
  check(ScanManager.hasBeenScanned(p,item(stack("wool",7))),"wire roundtrip restores wildcard");
  int objects=k.objectsScanned.get(name).size(),entities=k.entitiesScanned.get(name).size();check(ScanAll.complete(p)==0,"repeat command discovers nothing new");
  check(k.objectsScanned.get(name).size()==objects&&k.entitiesScanned.get(name).size()==entities,"repeat command creates no duplicate records");
  verify(p,before);
  k.objectsScanned.get(name).remove(AllScanHistory.MARKER);check(!ScanManager.hasBeenScanned(p,item(stack("clock",0))),"removing marker restores ordinary scans");
  check(k.objectsScanned.get(name).contains("@12345"),"removing marker preserves real history");
 }
}
