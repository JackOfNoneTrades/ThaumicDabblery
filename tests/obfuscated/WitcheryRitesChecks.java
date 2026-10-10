package tdtest;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.*;
import net.minecraft.world.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.*;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.*;
import net.minecraft.init.*;
import net.minecraft.util.*;
import net.minecraftforge.common.config.Configuration;
import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.ritual.*;
import com.emoniph.witchery.blocks.BlockCircle.TileEntityCircle;
import com.emoniph.witchery.common.*;
import com.emoniph.witchery.util.Coord;
import minetweaker.MineTweakerImplementationAPI;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.WitcheryRitesZen;
import org.fentanylsolutions.thaumicdabblery.feature.witcheryrites.WitcheryRitesFeature;
import org.fentanylsolutions.thaumicdabblery.mixins.late.witchery.*;

public final class WitcheryRitesChecks {
 public int checks;
 private final List<RiteRegistry.Ritual> originals=new ArrayList<>();
 private final List<Sacrifice> sacrifices=new ArrayList<>();private final List<Circle[]> circles=new ArrayList<>();private final List<EnumSet<RitualTraits>> traits=new ArrayList<>();
 public void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 private RitualAccessor data(int id){return (RitualAccessor)RiteRegistry.instance().getRitual((byte)id);}
 public void snapshot(){for(RiteRegistry.Ritual r:RiteRegistry.instance().getRituals()){originals.add(r);RitualAccessor d=(RitualAccessor)r;sacrifices.add(d.td$getSacrifice());circles.add(d.td$getCircles());traits.add(d.td$getTraits().clone());}}
 private void stable(boolean restored){check(originals.size()==RiteRegistry.instance().getRituals().size(),"registry size stable");for(int i=0;i<originals.size();i++){RiteRegistry.Ritual r=RiteRegistry.instance().getRitual((byte)(i+1));check(r==originals.get(i),"ritual identity and ID stable "+i);if(restored||i!=2){RitualAccessor d=(RitualAccessor)r;check(d.td$getSacrifice()==sacrifices.get(i),"original sacrifices restored "+i);check(d.td$getCircles()==circles.get(i),"original circles restored "+i);check(d.td$getTraits().equals(traits.get(i)),"original traits restored "+i);}}}
 private List<Sacrifice> parts(Sacrifice root){List<Sacrifice> out=new ArrayList<>();if(root.getClass()==SacrificeMultiple.class){for(Sacrifice s:((SacrificeMultipleAccessor)root).td$getSacrifices())out.addAll(parts(s));}else out.add(root);return out;}
 private <T> List<T> types(int id,Class<T> c){List<T> out=new ArrayList<>();for(Sacrifice s:parts(data(id).td$getSacrifice()))if(s.getClass()==c)out.add(c.cast(s));return out;}
 private void fixture(){check(types(3,SacrificePower.class).get(0).powerRequired==25,"script power");check(types(3,SacrificeLiving.class).size()==1,"script living");check(((SacrificeLivingAccessor)types(3,SacrificeLiving.class).get(0)).td$getEntityClass()==EntityPig.class,"pig class");check(((SacrificeItemAccessor)types(3,SacrificeItem.class).get(0)).td$getItems()[0].func_77973_b()==Items.field_151045_i,"script diamond");check(types(3,SacrificeOptionalItem.class).size()==1,"optional retained separately");check(Arrays.equals(RiteRegistry.instance().getRitual((byte)3).getCircles(),new byte[]{6}),"native book circle texture");check(RiteRegistry.instance().getRitual((byte)3).getDescription().contains("Diamond"),"native book item text");check(data(3).td$getTraits().equals(EnumSet.of(RitualTraits.ONLY_AT_NIGHT,RitualTraits.ONLY_IN_STROM,RitualTraits.ONLY_OVERWORLD)),"native traits");}
 private void script(String text)throws Exception{Files.write(Paths.get("scripts/zz-rites-checks.zs"),text.getBytes(StandardCharsets.UTF_8));MineTweakerImplementationAPI.reload();}
 public void registry()throws Exception{
  check(Boolean.FALSE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment")),"production obfuscated environment");
  fixture();stable(false);for(int i=0;i<2;i++){MineTweakerImplementationAPI.reload();fixture();stable(false);}
  String bad="import mods.witchery.Rites;\nRites.setPower(3,-1);Rites.setPower(999,20);Rites.setItems(3,[<minecraft:diamond>*2]);Rites.setItems(3,[<minecraft:diamond>.withTag({x:1})]);Rites.setItems(3,[<minecraft:wool:*>]);Rites.setLivingSacrifices(3,[\"Player\"]);Rites.setLivingSacrifices(3,[\"missing:mob\"]);Rites.setCircles(3,[\"small white\",\"small infernal\"]);Rites.setCircles(3,[\"huge white\"]);Rites.setWeather(3,\"clear\");Rites.setTime(3,\"later\");Rites.setPower(2,7);";
  script(bad);fixture();check(types(2,SacrificePower.class).get(0).powerRequired==7,"valid line after invalid definitions");script("");stable(false);
  script("import mods.witchery.Rites; Rites.setItems(3,[]);Rites.setOptionalItems(3,[]);Rites.setLivingSacrifices(3,[]);Rites.setPower(3,0);Rites.setCircles(3,[]);Rites.setTime(3,\"any\");Rites.setWeather(3,\"any\");Rites.setOverworldOnly(3,false);");
  check(types(3,SacrificePower.class).isEmpty()&&types(3,SacrificeLiving.class).isEmpty()&&types(3,SacrificeOptionalItem.class).isEmpty(),"requirements removed");check(data(3).td$getCircles().length==0&&data(3).td$getTraits().isEmpty(),"conditions removed");check(((SacrificeItemAccessor)types(3,SacrificeItem.class).get(0)).td$getItems().length==0,"items removed");script("");fixture();
  script("import mods.witchery.Rites;Rites.setItems(3,[<minecraft:cookie>]);Rites.setItems(3,[<minecraft:apple>]);Rites.setPower(3,1);Rites.setPower(3,2);Rites.setLivingSacrifices(3,[\"Pig\",\"Pig\",\"Cow\"]);Rites.setCircles(3,[\"small otherwhere\",\"medium infernal\",\"large white\"]);");
  check(types(3,SacrificePower.class).get(0).powerRequired==2,"last setter wins");check(types(3,SacrificeLiving.class).size()==3,"multiple living sacrifices");check(Arrays.equals(RiteRegistry.instance().getRitual((byte)3).getCircles(),new byte[]{7,5,0}),"three native circles");script("");fixture();
  Path path=Paths.get("scripts/witchery-rites.zs");byte[] content=Files.readAllBytes(path);Files.write(path,new byte[0]);MineTweakerImplementationAPI.reload();stable(true);Files.write(path,content);MineTweakerImplementationAPI.reload();fixture();
  Configuration cfg=new Configuration();cfg.get("features.witcheryrites","enabled",true).set(false);new WitcheryRitesFeature().configure(cfg);MineTweakerImplementationAPI.reload();stable(true);cfg.get("features.witcheryrites","enabled",true).set(true);new WitcheryRitesFeature().configure(cfg);MineTweakerImplementationAPI.reload();fixture();
  // Unknown requirement subclasses and native power timing survive targeted edits.
  Sacrifice old=data(3).td$getSacrifice();Sacrifice marker=new Sacrifice(){public boolean isMatch(World w,int x,int y,int z,int r,ArrayList<Entity> e,ArrayList<ItemStack> s){return true;}};
  data(3).td$setSacrifice(new SacrificeMultiple(marker,new SacrificeMultiple(new SacrificePower(10,60),new SacrificeOptionalItem(new ItemStack(Items.field_151106_aX)))));
  WitcheryRitesZen.setPower(3,12);WitcheryRitesZen.setItems(3,new minetweaker.api.item.IItemStack[0]);check(parts(data(3).td$getSacrifice()).contains(marker),"unknown requirement preserved");check(types(3,SacrificeOptionalItem.class).size()==1,"optional not removed by mandatory edit");check(types(3,SacrificePower.class).get(0).powerFrequencyInTicks==60,"power scheduling retained");MineTweakerImplementationAPI.reload();data(3).td$setSacrifice(old);fixture();
  EnumSet<RitualTraits> original2=data(2).td$getTraits();EnumSet<RitualTraits> shared=data(3).td$getTraits();data(2).td$setTraits(shared);WitcheryRitesZen.setTime(2,"day");check(data(3).td$getTraits()==shared&&shared.contains(RitualTraits.ONLY_AT_NIGHT)&&!shared.contains(RitualTraits.ONLY_AT_DAY),"shared condition set isolated");MineTweakerImplementationAPI.reload();check(data(2).td$getTraits()==shared,"undo restores exact shared set");data(2).td$setTraits(original2);
  WitcheryRitesZen.dump();
 }
 public void runtime(EntityPlayerMP player)throws Exception{
  WorldServer world=MinecraftServer.func_71276_C().func_71218_a(0);RiteRegistry.Ritual ritual=RiteRegistry.instance().getRitual((byte)3);
  int x=20,y=100,z=20;world.func_72964_e(x>>4,z>>4);AxisAlignedBB bounds=AxisAlignedBB.func_72330_a(x-5,y-1,z-5,x+5,y+3,z+5);
  for(Object o:world.func_72872_a(Entity.class,bounds))if(o instanceof EntityItem||o instanceof EntityLiving)((Entity)o).func_70106_y();
  EntityItem diamond=new EntityItem(world,x+.5,y+.1,z+.5,new ItemStack(Items.field_151045_i));
  ArrayList<Entity> input=new ArrayList<>();input.add(diamond);Circle[] ring={new Circle(16,0,0)};
  check(!ritual.isMatch(world,x,y,z,ring,new ArrayList<>(input),new ArrayList<ItemStack>(),true,true,true),"day rejected");
  check(!ritual.isMatch(world,x,y,z,ring,new ArrayList<>(input),new ArrayList<ItemStack>(),false,true,false),"rain without thunder rejected");
  check(!ritual.isMatch(world,x,y,z,new Circle[0],new ArrayList<>(input),new ArrayList<ItemStack>(),false,true,true),"missing circle rejected");
  check(!ritual.isMatch(world,x,y,z,new Circle[]{new Circle(0,16,0)},new ArrayList<>(input),new ArrayList<ItemStack>(),false,true,true),"wrong chalk rejected");
  check(!ritual.isMatch(world,x,y,z,ring,new ArrayList<Entity>(),new ArrayList<ItemStack>(),false,true,true),"missing item rejected");
  check(!ritual.isMatch(MinecraftServer.func_71276_C().func_71218_a(-1),x,y,z,ring,new ArrayList<>(input),new ArrayList<ItemStack>(),false,true,true),"Overworld restriction");
  check(ritual.isMatch(world,x,y,z,ring,new ArrayList<>(input),new ArrayList<ItemStack>(),false,true,true),"valid offering without optional cookie accepted");
  Power power=new Power(world,x,y,z);PowerSources.instance().registerPowerSource(power);
  try{
   // Real Witchery tile executes all native sacrifice/effect steps, with controlled power source.
   world.func_147465_d(x,y-1,z,Blocks.field_150348_b,0,3);world.func_147465_d(x,y,z,Witchery.Blocks.CIRCLE,0,3);
   TileEntityCircle tile=(TileEntityCircle)world.func_147438_o(x,y,z);
   EntityPig pig=new EntityPig(world);pig.func_70107_b(x+1,y,z);world.func_72838_d(pig);world.func_72838_d(diamond);
   power.amount=24;tile.queueRitual(ritual,bounds,player,0,false);run(tile);check(!pig.field_70128_L,"insufficient power stops before living sacrifice");check(output(world,bounds)==0,"insufficient power has no output");check(power.amount==24,"failed power debit atomic");
   clearItems(world,bounds);diamond=new EntityItem(world,x+.5,y+.1,z+.5,new ItemStack(Items.field_151045_i));world.func_72838_d(diamond);power.amount=100;tile.queueRitual(ritual,bounds,player,0,false);run(tile);check(pig.field_70128_L,"pig sacrificed");check(diamond.field_70128_L,"mandatory item consumed");check(power.amount==75,"exact edited initial power consumed");check(output(world,bounds)==1,"native charged attuned stone produced");
   clearItems(world,bounds);world.func_72838_d(new EntityItem(world,x+.5,y+.1,z+.5,new ItemStack(Items.field_151045_i)));power.amount=100;tile.queueRitual(ritual,bounds,player,0,false);run(tile);check(output(world,bounds)==0,"missing living sacrifice aborts native effect");
   clearItems(world,bounds);EntityPig pig2=new EntityPig(world);pig2.func_70107_b(x+1,y,z);world.func_72838_d(pig2);world.func_72838_d(new EntityItem(world,x+.5,y+.1,z+.5,new ItemStack(Items.field_151045_i)));EntityItem cookie=new EntityItem(world,x+.5,y+.1,z+.5,new ItemStack(Items.field_151106_aX));world.func_72838_d(cookie);power.amount=100;tile.queueRitual(ritual,bounds,player,0,false);run(tile);check(cookie.field_70128_L&&output(world,bounds)==1,"optional offering consumed when present");
   clearItems(world,bounds);
   // No initial cost means no altar requirement; optional/mob removal also affects native execution.
   script("import mods.witchery.Rites;Rites.setPower(3,0);Rites.setLivingSacrifices(3,[]);Rites.setOptionalItems(3,[]);");PowerSources.instance().removePowerSource(power);world.func_72838_d(new EntityItem(world,x+.5,y+.1,z+.5,new ItemStack(Items.field_151045_i)));tile.queueRitual(ritual,bounds,player,0,false);run(tile);check(output(world,bounds)==1,"zero cost completes without altar or mob");clearItems(world,bounds);
   script("import mods.witchery.Rites;Rites.setPower(3,0);Rites.setLivingSacrifices(3,[]);Rites.setOptionalItems(3,[]);Rites.setTime(3,\"any\");Rites.setWeather(3,\"any\");");
   Method activate=com.emoniph.witchery.blocks.BlockCircle.class.getDeclaredMethod("activateBlock",World.class,int.class,int.class,int.class,net.minecraft.entity.player.EntityPlayer.class,boolean.class);activate.setAccessible(true);
   for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){world.func_147465_d(x+dx,y-1,z+dz,Blocks.field_150348_b,0,3);if(dx!=0||dz!=0)world.func_147468_f(x+dx,y,z+dz);}
   world.func_72838_d(new EntityItem(world,x+.5,y+.1,z+.5,new ItemStack(Items.field_151045_i)));activate.invoke(Witchery.Blocks.CIRCLE,world,x,y,z,player,false);check(!tile.isRitualActive(),"native scanner rejects missing ring");
   for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)if((Math.abs(dx)==3&&Math.abs(dz)<=1)||(Math.abs(dz)==3&&Math.abs(dx)<=1)||(Math.abs(dx)==2&&Math.abs(dz)==2))world.func_147465_d(x+dx,y,z+dz,Witchery.Blocks.GLYPH_RITUAL,0,3);
   activate.invoke(Witchery.Blocks.CIRCLE,world,x,y,z,player,false);check(tile.isRitualActive(),"native scanner activates edited rite");run(tile);check(output(world,bounds)==1,"native activation produces unchanged output");script("");
  }finally{PowerSources.instance().removePowerSource(power);clearItems(world,bounds);}
 }
 private void run(TileEntityCircle tile){for(int i=0;i<500&&tile.isRitualActive();i++){tile.func_145845_h();World w=tile.func_145831_w();for(Object o:new ArrayList(w.field_72996_f))if(((Entity)o).field_70128_L)w.func_72973_f((Entity)o);}check(!tile.isRitualActive(),"ritual terminates");}
 private int output(World w,AxisAlignedBB box){int n=0;for(Object o:w.func_72872_a(EntityItem.class,box)){EntityItem e=(EntityItem)o;if(!e.field_70128_L&&Witchery.Items.GENERIC.itemAttunedStoneCharged.isMatch(e.func_92059_d()))n+=e.func_92059_d().field_77994_a;}return n;}
 private void clearItems(World w,AxisAlignedBB box){for(Object o:w.func_72872_a(EntityItem.class,box))w.func_72973_f((EntityItem)o);}
 private static final class Power implements IPowerSource{
  final World world;final Coord location;float amount;
  Power(World w,int x,int y,int z){world=w;location=new Coord(x,y,z);}
  public World getWorld(){return world;}public Coord getLocation(){return location;}public boolean isLocationEqual(Coord c){return location.equals(c);}public boolean consumePower(float v){if(amount<v)return false;amount-=v;return true;}public float getCurrentPower(){return amount;}public float getRange(){return 16;}public int getEnhancementLevel(){return 0;}public boolean isPowerInvalid(){return false;}
 }
}
