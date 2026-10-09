package tdtest;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import net.minecraft.world.WorldServer;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import com.kentington.thaumichorizons.common.ThaumicHorizons;
import com.kentington.thaumichorizons.common.tiles.TileVortex;
import org.fentanylsolutions.thaumicdabblery.feature.planarvortex.VortexRecipes;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.PlanarVortexZen;
import thaumcraft.common.config.ConfigItems;
import minetweaker.MineTweakerImplementationAPI;
import minetweaker.api.minecraft.MineTweakerMC;
public final class PlanarVortexChecks {
 public int checks;private WorldServer world;private TileVortex vat;private Method craft;
 public static final String PREFIX="import mods.thaumichorizons.PlanarVortex;\n";
 public static final String DEMO=PREFIX+"PlanarVortex.addItemRecipe(\"custom:diamond\", <minecraft:iron_ingot> * 2, <minecraft:diamond>.withTag({display: {Name: \"Void Diamond\"}}));\nPlanarVortex.addEntityRecipe(\"custom:zombie\", <minecraft:rotten_flesh> * 4, \"Zombie\", {CustomName: \"The Visitor\", PersistenceRequired: 1 as byte, IsBaby: 0 as byte});\n";
 public void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
 public static void script(String text)throws Exception{Files.createDirectories(Paths.get("scripts"));Files.write(Paths.get("scripts/zz-planar-vortex.zs"),text.getBytes(StandardCharsets.UTF_8));MineTweakerImplementationAPI.reload();}
 public static TileVortex vortex(WorldServer w,int x,int y,int z)throws Exception{w.func_147465_d(x,y,z,ThaumicHorizons.blockVortex,0,3);TileVortex v=(TileVortex)w.func_147438_o(x,y,z);v.aspects.add(thaumcraft.api.aspects.Aspect.ENTROPY,20);v.cheat=true;v.count=50;v.beams=6;java.lang.reflect.Field f=TileVortex.class.getDeclaredField("ateDevices");f.setAccessible(true);f.setBoolean(v,true);return v;}
 private EntityItem drop(ItemStack stack){EntityItem item=new EntityItem(world,vat.field_145851_c+.5,vat.field_145848_d+.5,vat.field_145849_e+.5,stack);item.field_145804_b=100;world.func_72838_d(item);return item;}
 private void apply(EntityItem item)throws Exception{craft.invoke(vat,item);finish(vat);}
 public static void finish(TileVortex tile){if(org.fentanylsolutions.thaumicdabblery.feature.planarvortex.VortexCrafting.state(tile).started<0)return;for(int i=0;i<36;i++){tile.func_145831_w().func_72912_H().func_82572_b(tile.func_145831_w().func_82737_E()+1);org.fentanylsolutions.thaumicdabblery.feature.planarvortex.VortexCrafting.tick(tile);}}
 private int total(){int n=0;for(ItemStack s:vat.items)n+=s.field_77994_a;return n;}
 private void invalid(Runnable action){try{action.run();throw new AssertionError("accepted invalid recipe");}catch(IllegalArgumentException expected){checks++;}}
 private List<Entity> nearby(){return world.func_72839_b(null,AxisAlignedBB.func_72330_a(38,98,38,43,104,43));}
 public static final class CancelZombie { @SubscribeEvent public void spawn(EntityJoinWorldEvent e){if(e.entity instanceof EntityZombie)e.setCanceled(true);} }
 public void run(WorldServer w)throws Exception{
  world=w;check(Boolean.FALSE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment")),"obfuscated runtime");
  craft=TileVortex.class.getDeclaredMethod("handleVoidCrafting",EntityItem.class);craft.setAccessible(true);vat=vortex(w,40,100,40);
  script(DEMO);check(VortexRecipes.RECIPES.size()==2,"both script overloads registered");
  EntityItem iron=drop(new ItemStack(Items.field_151042_j,5));vat.func_145845_h();check(total()==2&&iron.func_92059_d().field_77994_a==1&&!iron.field_70128_L,"real tick consumes complete batches and leaves remainder");check(vat.items.get(0).func_77973_b()==Items.field_151045_i&&vat.items.get(0).func_82833_r().equals("Void Diamond"),"item output retains NBT");
  vat.func_145845_h();check(total()==2,"partial input not processed twice");iron.func_70106_y();
  NBTTagCompound save=new NBTTagCompound();vat.func_145841_b(save);TileVortex restored=new TileVortex();restored.func_145839_a(save);check(restored.items.size()==2&&restored.items.get(0).func_82833_r().equals("Void Diamond"),"queued outputs survive tile save/load");
  int before=nearby().size();net.minecraft.entity.player.EntityPlayerMP fp=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(w);ItemStack fw=new ItemStack(ConfigItems.itemWandCasting);fp.field_71071_by.func_70299_a(0,fw);w.field_73010_i.add(fp);vat.onWandRightClick(w,fw,fp,40,100,40,1,0);finish(vat);w.field_73010_i.remove(fp);check(vat.items.size()==1,"native wand retrieves one queued output");boolean retrieved=false;for(Entity e:nearby())if(e instanceof EntityItem&&((EntityItem)e).func_92059_d().func_77973_b()==Items.field_151045_i)retrieved=true;check(retrieved,"native wand creates real output item");vat.items.clear();
  EntityItem flesh=drop(new ItemStack(Items.field_151078_bh,9));vat.func_145845_h();finish(vat);List<EntityZombie> zombies=new ArrayList<>();for(Entity e:nearby())if(e instanceof EntityZombie)zombies.add((EntityZombie)e);check(zombies.size()==2&&flesh.func_92059_d().field_77994_a==1,"one entity per complete input batch");check(!zombies.get(0).func_110124_au().equals(zombies.get(1).func_110124_au()),"fresh entity identities");for(EntityZombie z:zombies){check(z.func_94057_bL().equals("The Visitor"),"entity NBT name applied");check(z.field_70165_t==40.5&&z.field_70163_u==100.5,"entity spawns at vortex");z.func_70106_y();}flesh.func_70106_y();
  CancelZombie cancel=new CancelZombie();MinecraftForge.EVENT_BUS.register(cancel);try{EntityItem rejected=drop(new ItemStack(Items.field_151078_bh,4));apply(rejected);check(!rejected.field_70128_L&&rejected.func_92059_d().field_77994_a==4,"cancelled entity spawn retains input");rejected.func_70106_y();}finally{MinecraftForge.EVENT_BUS.unregister(cancel);}
  for(int i=0;i<3;i++){script(DEMO);check(VortexRecipes.RECIPES.size()==2,"reload replaces rather than duplicates");}
  script(PREFIX+"PlanarVortex.addEntityRecipe(\"custom:pig\", <minecraft:apple>, \"Pig\");\n");check(VortexRecipes.RECIPES.size()==1&&VortexRecipes.RECIPES.containsKey("custom:pig"),"three-argument entity overload and reload undo");
  script(PREFIX+"PlanarVortex.addItemRecipe(\"custom:wool\", <minecraft:wool:*> * 2, <minecraft:diamond> * 3);\nPlanarVortex.addItemRecipe(\"custom:red\", <minecraft:wool:14>, <minecraft:emerald>);\n");EntityItem red=drop(new ItemStack((net.minecraft.block.Block)net.minecraft.block.Block.field_149771_c.func_82594_a("wool"),1,14));apply(red);check(red.field_70128_L&&vat.items.get(0).func_77973_b()==Items.field_151166_bC,"last matching recipe wins");vat.items.clear();EntityItem blue=drop(new ItemStack((net.minecraft.block.Block)net.minecraft.block.Block.field_149771_c.func_82594_a("wool"),5,11));apply(blue);check(total()==6&&blue.func_92059_d().field_77994_a==1,"wildcard metadata and output count");blue.func_70106_y();vat.items.clear();
  script(PREFIX+"PlanarVortex.addItemRecipe(\"custom:tag\", <minecraft:stick>.withTag({key: \"yes\"}), <minecraft:diamond>);\n");EntityItem plain=drop(new ItemStack(Items.field_151055_y));apply(plain);check(!plain.field_70128_L&&vat.items.isEmpty(),"tagged input rejects plain item");plain.func_70106_y();ItemStack tagged=new ItemStack(Items.field_151055_y);NBTTagCompound tag=new NBTTagCompound();tag.func_74778_a("key","yes");tagged.func_77982_d(tag);EntityItem match=drop(tagged);apply(match);check(match.field_70128_L&&total()==1,"exact input NBT matches");vat.items.clear();
  script(PREFIX+"PlanarVortex.addItemRecipe(\"custom:swords\", <minecraft:stick>, <minecraft:iron_sword> * 3);\n");EntityItem swordInput=drop(new ItemStack(Items.field_151055_y));apply(swordInput);check(vat.items.size()==3,"non-stackable output split into legal stacks");for(ItemStack s:vat.items)check(s.field_77994_a==1,"output max stack respected");vat.items.clear();
  script(PREFIX+"PlanarVortex.addItemRecipe(\"custom:override\", <Thaumcraft:ItemResource:16> * 2, <minecraft:diamond>);\n");EntityItem partial=drop(new ItemStack(ConfigItems.itemResource,1,16));apply(partial);check(!partial.field_70128_L&&vat.items.isEmpty(),"partial custom input never falls through to native putty");partial.func_70106_y();
  String[] keys={"void_putty","wisps","crystal_wand","void_golem"};ItemStack[] inputs={new ItemStack(ConfigItems.itemResource,2,16),new ItemStack(ConfigItems.itemResource,1,14),new ItemStack(ThaumicHorizons.itemCrystalWand),new ItemStack(ThaumicHorizons.itemGolemPowder)};
  StringBuilder removals=new StringBuilder(PREFIX);for(String key:keys)removals.append("PlanarVortex.removeRecipe(\"builtin:").append(key).append("\");\n");script(removals.toString());check(VortexRecipes.DISABLED.size()==4,"all native removals scripted");for(ItemStack input:inputs){EntityItem item=drop(input.func_77946_l());item.func_145799_b("VortexTester");apply(item);check(!item.field_70128_L,"removed native recipe leaves input");item.func_70106_y();}check(vat.items.isEmpty(),"removed recipes produce no items");
  script("");check(VortexRecipes.RECIPES.isEmpty()&&VortexRecipes.DISABLED.isEmpty(),"script removal restores native registry");for(int i=0;i<inputs.length;i++){EntityItem item=drop(inputs[i].func_77946_l());item.func_145799_b("VortexTester");apply(item);check(item.field_70128_L,"native recipe restored: "+keys[i]);}check(vat.items.get(0).func_77973_b()==ThaumicHorizons.itemVoidPutty&&vat.items.get(0).field_77994_a==2,"native putty unchanged");check(vat.items.get(1).func_77973_b()==ThaumicHorizons.itemWandCastingDisposable&&vat.items.get(1).func_77942_o(),"native charged wand retains special NBT");for(thaumcraft.api.aspects.Aspect aspect:new thaumcraft.api.aspects.Aspect[]{thaumcraft.api.aspects.Aspect.AIR,thaumcraft.api.aspects.Aspect.EARTH,thaumcraft.api.aspects.Aspect.FIRE,thaumcraft.api.aspects.Aspect.WATER,thaumcraft.api.aspects.Aspect.ORDER,thaumcraft.api.aspects.Aspect.ENTROPY})check(((thaumcraft.common.items.wands.ItemWandCasting)vat.items.get(1).func_77973_b()).getVis(vat.items.get(1),aspect)==25000,"native wand full vis");int wisps=0,golems=0;for(Entity e:nearby()){if(e instanceof thaumcraft.common.entities.monster.EntityWisp)wisps++;if(e instanceof com.kentington.thaumichorizons.common.entities.EntityGolemTH){golems++;check(((com.kentington.thaumichorizons.common.entities.EntityGolemTH)e).getOwnerName().equals("VortexTester"),"native golem retains thrower ownership");}}check(wisps>=1&&wisps<=4&&golems==1,"native random wisps and golem spawned");vat.items.clear();
  script(DEMO+"PlanarVortex.removeRecipe(\"custom:diamond\");\n");check(!VortexRecipes.RECIPES.containsKey("custom:diamond"),"custom removal");script(DEMO);check(VortexRecipes.RECIPES.size()==2,"custom removal undo and re-registration");
  EntityItem noCraft=drop(new ItemStack(Items.field_151042_j,2));vat.createdDimension=true;vat.func_145845_h();check(!noCraft.field_70128_L&&vat.items.isEmpty(),"portal does not craft");vat.createdDimension=false;vat.cheat=false;vat.beams=5;vat.func_145845_h();check(vat.items.isEmpty(),"unstabilized vortex does not craft");vat.cheat=true;vat.beams=6;noCraft.func_70106_y();
  minetweaker.api.item.IItemStack valid=MineTweakerMC.getIItemStack(new ItemStack(Items.field_151045_i));
  invalid(()->PlanarVortexZen.addItemRecipe("bad",valid,valid));invalid(()->PlanarVortexZen.addItemRecipe("custom:diamond",valid,valid));invalid(()->PlanarVortexZen.addItemRecipe("custom:null",null,valid));invalid(()->PlanarVortexZen.addEntityRecipe("custom:badmob",valid,"MissingMob"));invalid(()->PlanarVortexZen.removeRecipe("builtin:missing"));invalid(()->PlanarVortexZen.addItemRecipe("custom:pearl",MineTweakerMC.getIItemStack(new ItemStack(ConfigItems.itemEldritchObject,1,3)),valid));invalid(()->PlanarVortexZen.addItemRecipe("custom:zero",MineTweakerMC.getIItemStack(new ItemStack(Items.field_151045_i,0)),valid));
  for(String key:new String[]{"id","Pos","Motion","Rotation","Dimension","UUIDMost","UUIDLeast","Riding"}){NBTTagCompound forbidden=new NBTTagCompound();forbidden.func_74778_a(key,"blocked");invalid(()->PlanarVortexZen.addEntityRecipe("custom:forbidden",valid,"Zombie",minetweaker.mc1710.data.NBTConverter.from(forbidden,false)));}
  invalid(()->PlanarVortexZen.addEntityRecipe("custom:scalar",valid,"Zombie",minetweaker.mc1710.data.NBTConverter.from(new net.minecraft.nbt.NBTTagString("bad"),false)));
  invalid(()->PlanarVortexZen.addItemRecipe("custom:too_many",valid,MineTweakerMC.getIItemStack(new ItemStack(Items.field_151045_i,65))));
  script("");for(Entity e:nearby())e.func_70106_y();w.func_147468_f(40,100,40);
 }
}
