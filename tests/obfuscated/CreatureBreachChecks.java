package tdtest;

import java.util.*;
import java.nio.file.*;
import com.mojang.authlib.GameProfile;
import com.kentington.thaumichorizons.common.ThaumicHorizons;
import com.kentington.thaumichorizons.common.tiles.*;
import com.kentington.thaumichorizons.common.lib.*;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.*;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import thaumcraft.api.aspects.*;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TilePedestal;
import org.fentanylsolutions.thaumicdabblery.compat.thaumichorizons.CustomCreatureRecipe;

/** Real vat structures and server explosion/entity events, in a disposable obfuscated world only. */
public final class CreatureBreachChecks {
 public final CustomCreatureChecks c;
 public final WorldServer world;
 private final EntityPlayerMP player;
 public int x=40,y=100,z=0;
 public boolean watching;
 public int explosions,spawns,matrixDrops;
 public final List<String> sounds=new ArrayList<>();
 public EntityLiving released;
 public CreatureBreachChecks(CustomCreatureChecks checks,WorldServer world){c=checks;this.world=world;player=FakePlayerFactory.get(world,new GameProfile(UUID.randomUUID(),"BreachProbe"));}
 public static final String API="mods.thaumichorizons.CreatureInfusion.";
 public static String recipe(){return CustomCreatureChecks.add("custom:breach","","Pig","PigZombie","cookie");}
 public static String nbt(){return API+"setEntityNBT(\"custom:breach\", {CustomName:\"Subject\", ForgeData:{experiment:2}}, {CustomName:\"BreachOutput\", IsBaby:1 as byte, ForgeData:{result:7}, ActiveEffects:[{Id:1 as byte, Amplifier:1 as byte, Duration:600}]});\n";}
 public static String breach(float power){return API+"setBreach(\"custom:breach\", "+power+");\n";}
 public static String block(String mob,String key){return API+"blacklistInfusion(\""+mob+"\", \""+key+"\");\n";}
 public static NBTTagCompound data(Entity e){try{return (NBTTagCompound)e.getClass().getMethod("getEntityData").invoke(e);}catch(Exception ex){throw new RuntimeException(ex);}}
 public EntityPig subject(){EntityPig pig=new EntityPig(world);pig.func_94058_c("Subject");data(pig).func_74768_a("experiment",2);data(pig).func_74778_a("unrelated","retained only on input");return pig;}
 public TileVat loose(EntityLiving mob){TileVat vat=new TileVat();vat.func_145834_a(world);vat.field_145851_c=x;vat.field_145848_d=y;vat.field_145849_e=z;vat.setEntityContained(mob);pedestal();return vat;}
 public void pedestal(){world.func_147465_d(x+2,y-1,z,ConfigBlocks.blockStoneDevice,1,3);((TilePedestal)world.func_147438_o(x+2,y-1,z)).func_70299_a(0,CustomCreatureChecks.stack("cookie"));}
 public TileVat assembled(EntityLiving mob)throws Exception{
  for(int dy=0;dy<4;dy++)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
   boolean center=dx==0&&dz==0,cap=dy==0||dy==3;
   world.func_147465_d(x+dx,y-dy,z+dz,cap?(center?ConfigBlocks.blockMetalDevice:ConfigBlocks.blockWoodenDevice):(center?Blocks.field_150355_j:Blocks.field_150359_w),cap?(center?9:6):0,3);
  }
  Class<?> cls=Class.forName("com.kentington.thaumichorizons.common.items.WandManagerTH");java.lang.reflect.Method replace=cls.getDeclaredMethod("replaceVat",net.minecraft.world.World.class,int.class,int.class,int.class);replace.setAccessible(true);replace.invoke(cls.newInstance(),world,x-1,y-3,z-1);
  TileVat vat=(TileVat)world.func_147438_o(x,y,z);vat.setEntityContained(mob);pedestal();world.func_147465_d(x,y+1,z,ThaumicHorizons.blockModifiedMatrix,0,3);return vat;
 }
 public void start(TileVat vat){c.start(vat,world,player);}
 public void finish(TileVat vat)throws Exception{
  vat.instability=0;AspectList demand=(AspectList)CustomCreatureChecks.get(vat,"essentiaDemanded");for(Aspect a:demand.getAspects())if(a!=null)vat.addToContainer(a,demand.getAmount(a));
  for(int i=0;i<40&&vat.mode==2;i++)vat.craftCycle();c.check(vat.mode==0,"native infusion completed");
 }
 @SubscribeEvent public void explosion(ExplosionEvent.Start event){
  if(!watching||event.world!=world)return;explosions++;
  c.check(released==null&&spawns==0,"output not spawned before explosion");
  c.check(world.func_147437_c(x,y+1,z)&&matrixDrops==0,"matrix removed before blast but item not spawned yet");
  c.check(sounds.equals(Arrays.asList("dig.glass","liquid.water")),"glass and water sounds broadcast once before explosion");
  c.check(world.func_147438_o(x,y,z)==null,"controller dismantled before explosion");
  c.check(world.func_147437_c(x,y-1,z)&&world.func_147437_c(x,y-2,z),"both water blocks cleared before explosion");
  for(int dy=0;dy<4;dy++)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)c.check(!(world.func_147438_o(x+dx,y-dy,z+dz) instanceof TileVatSlave),"slave dismantled before explosion");
 }
 @SubscribeEvent public void spawn(EntityJoinWorldEvent event){
  if(watching&&event.world==world&&event.entity instanceof net.minecraft.entity.item.EntityItem){
   net.minecraft.item.ItemStack stack=((net.minecraft.entity.item.EntityItem)event.entity).func_92059_d();
   if(stack.func_77973_b()==net.minecraft.item.Item.func_150898_a(ThaumicHorizons.blockModifiedMatrix)){matrixDrops+=stack.field_77994_a;c.check(explosions==1,"matrix drop occurs after explosion");}
  }
  if(!watching||event.world!=world||!(event.entity instanceof EntityLiving)||!"BreachOutput".equals(((EntityLiving)event.entity).func_94057_bL()))return;
  spawns++;released=(EntityLiving)event.entity;
  c.check(explosions==1,"one explosion before output spawn");
  c.check(released.func_110143_aJ()==released.func_110138_aP(),"output never damaged by its breach explosion");
  c.check(released.field_70165_t==x+0.5&&released.field_70163_u==y-2&&released.field_70161_v==z+0.5,"output at lower water position");
 }
 public void run()throws Exception{
  MinecraftForge.EVENT_BUS.register(this);
  net.minecraft.world.IWorldAccess audio=(net.minecraft.world.IWorldAccess)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{net.minecraft.world.IWorldAccess.class},(proxy,method,args)->{
   if(method.getName().equals("equals"))return proxy==args[0];
   if(method.getName().equals("hashCode"))return System.identityHashCode(proxy);
   if(watching&&args!=null&&args.length==6&&("dig.glass".equals(args[0])||"liquid.water".equals(args[0])))sounds.add((String)args[0]);return null;
  });
  world.func_72954_a(audio);
  try{
   c.base.script(recipe()+nbt());TileVat vat=loose(new EntityPig(world));start(vat);c.check(vat.mode==0,"input name/NBT mismatch refuses start");
   EntityPig pig=subject();data(pig).func_74768_a("experiment",3);vat.setEntityContained(pig);start(vat);c.check(vat.mode==0,"nested input mismatch refuses start");
   pig=subject();vat.setEntityContained(pig);start(vat);c.check(vat.mode==2,"subset input matches despite extra NBT");finish(vat);
   EntityLiving result=(EntityLiving)vat.getEntityContained();c.check(result instanceof EntityPigZombie&&"BreachOutput".equals(result.func_94057_bL())&&((EntityPigZombie)result).func_70631_g_(),"output name and baby NBT applied without breach");
   c.check(data(result).func_74762_e("result")==7&&!data(result).func_74764_b("experiment"),"output compound applied without source data");
   c.check(result.func_70644_a(net.minecraft.potion.Potion.field_76424_c)&&result.func_70660_b(net.minecraft.potion.Potion.field_76424_c).func_76458_c()==1,"output NBT list applied");
   c.check(result.func_110138_aP()==20&&!result.func_110124_au().equals(pig.func_110124_au()),"species health and fresh UUID preserved");
   String set=CreatureInfusionChecks.set("custom:breach","",0,"exanimis 3","<minecraft:cookie>");
   c.base.script(recipe()+nbt()+breach(2)+set+CreatureInfusionChecks.remove("custom:breach")+set);Object[] out=(Object[])c.current("custom:breach").getRecipeOutput();
   c.check(((NBTTagCompound)out[1]).func_74760_g("breach")==2&&((NBTTagCompound)out[1]).func_74775_l("nbt").func_74779_i("CustomName").equals("BreachOutput"),"setRecipe and remove/redefine retain NBT and breach settings");
   ((NBTTagCompound)out[1]).func_74775_l("nbt").func_74778_a("CustomName","mutated");c.check(((NBTTagCompound)((Object[])c.current("custom:breach").getRecipeOutput())[1]).func_74775_l("nbt").func_74779_i("CustomName").equals("BreachOutput"),"captured output NBT is independent copy");
   c.base.script(recipe()+nbt()+breach(2)+breach(0));vat=loose(subject());start(vat);finish(vat);c.check(vat.getEntityContained() instanceof EntityPigZombie,"zero disables breach");
   for(String bad:new String[]{API+"setEntityNBT(\"custom:breach\", 1, {});",API+"setEntityNBT(\"custom:breach\", {}, {UUIDMost:1 as long});",breach(-1),breach(33)}){
    c.base.script(recipe()+bad);c.check(((NBTTagCompound)((Object[])c.current("custom:breach").getRecipeOutput())[1]).func_74760_g("breach")==0,"invalid setting leaves original recipe");
   }
   // A failed NBT-specific recipe must let a later matching recipe win.
   c.base.script(recipe()+nbt()+CustomCreatureChecks.add("custom:fallback","","Pig","Cow","cookie"));vat=loose(new EntityPig(world));start(vat);finish(vat);c.check(vat.getEntityContained() instanceof EntityCow,"NBT mismatch falls through in recipe order");
   String upgrade=CreatureInfusionChecks.set("upgrade:4","",0,"","<minecraft:cookie>");
   c.base.script(upgrade+block("Pig","upgrade:4")+block("Pig","upgrade:4"));vat=loose(new EntityPig(world));start(vat);c.check(vat.mode==0&&((TilePedestal)world.func_147438_o(x+2,y-1,z)).func_70301_a(0)!=null,"duplicate blacklist refuses upgrade before item consumption");
   vat.setEntityContained(new EntityCow(world));start(vat);finish(vat);c.check(c.props(vat.getEntityContained()).hasInfusion(4),"other species still receives blacklisted upgrade");
   c.base.script(block("Pig","upgrade:4")+upgrade+CreatureInfusionChecks.remove("upgrade:4")+upgrade);vat=loose(new EntityPig(world));start(vat);c.check(vat.mode==0,"blacklist follows recipe replacement and restoration");
   c.base.script(upgrade);vat=loose(new EntityPig(world));start(vat);finish(vat);c.check(c.props(vat.getEntityContained()).hasInfusion(4),"removing duplicate blacklist restores eligibility");
   c.base.script(CreatureInfusionChecks.set("upgrade:7","",0,"","<minecraft:cookie>")+block("Pig","upgrade:7"));vat=loose(subject());start(vat);c.check(vat.mode==0,"loyalty blocked for exact species");
   c.base.script(recipe()+block("Pig","custom:breach"));vat=loose(subject());start(vat);c.check(vat.mode==0,"custom transformations can be blacklisted");
   c.base.script(CreatureInfusionChecks.set("transform:Cow->ThaumicHorizons.ChocolateCow","",0,"","<minecraft:cookie>")+block("Cow","transform:Cow->ThaumicHorizons.ChocolateCow"));vat=loose(new EntityCow(world));start(vat);c.check(vat.mode==0,"native transformations can be blacklisted");
   c.base.script(upgrade+block("Cow","upgrade:4"));vat=loose(new EntityMooshroom(world));start(vat);finish(vat);c.check(c.props(vat.getEntityContained()).hasInfusion(4),"blacklist does not include subclasses");
   c.base.script(recipe()+block("Pig","custom:breach")+CustomCreatureChecks.add("custom:fallback","","Pig","Cow","cookie"));vat=loose(subject());start(vat);finish(vat);c.check(vat.getEntityContained() instanceof EntityCow,"blocked recipe falls through to next match");
   c.base.script(recipe()+nbt()+breach(2));vat=assembled(subject());start(vat);c.check(vat.mode==2,"assembled vat starts breach recipe");
   NBTTagCompound saved=new NBTTagCompound();vat.func_145841_b(saved);CompressedStreamTools.func_74795_b(saved,Paths.get("pending-breach-vat.dat").toFile());
   c.base.script("");vat.func_145839_a(saved); // in-flight result no longer depends on registered recipe
   watching=true;finish(vat);watching=false;
   c.check(matrixDrops==1,"exactly one matrix item survives breach");
   c.check(explosions==1&&spawns==1&&released!=null&&vat.getEntityContained()==null,"one explosion and one released output, no contained duplicate");
   c.check(data(released).func_74762_e("result")==7&&c.props(released).getInfusionCosts().getAmount(Aspect.UNDEAD)==8,"saved result NBT and cost survive script removal");
   for(int dx=-4;dx<=4;dx++)for(int dy=-4;dy<=1;dy++)for(int dz=-4;dz<=4;dz++)c.check(world.func_147439_a(x+dx,y+dy,z+dz)!=ConfigBlocks.blockFluxGoo&&world.func_147439_a(x+dx,y+dy,z+dz)!=ConfigBlocks.blockFluxGas,"breach does not killSubject or create flux");
   // A missing output must not dismantle the vat or explode.
   x+=24;c.base.script(recipe()+breach(2));vat=assembled(subject());start(vat);((NBTTagCompound)CustomCreatureChecks.get(vat,"recipeOutput")).func_74778_a("entity","MissingOutput");EntityLivingBase original=vat.getEntityContained();finish(vat);c.check(world.func_147438_o(x,y,z)==vat&&vat.getEntityContained()==original,"unavailable output preserves assembled vat and input");
   vat.setEntityContained(null);((CustomCreatureRecipe.BreachVat)vat).thaumicdabblery$dismantleForBreach();
   x+=24;c.base.script(recipe().replace("PigZombie","Giant")+API+"setEntityNBT(\"custom:breach\", {}, {CustomName:\"BreachOutput\"});\n"+breach(4));vat=assembled(subject());start(vat);
   explosions=spawns=matrixDrops=0;sounds.clear();released=null;watching=true;finish(vat);watching=false;
   c.check(matrixDrops==1,"matrix survives larger breach explosion too");
   c.check(released instanceof EntityGiantZombie&&released.field_70131_O>2&&spawns==1&&vat.getEntityContained()==null,"oversized mob released outside vat with no containment");
   c.base.script("");c.check(((Map<?,?>)CustomCreatureChecks.get(CustomCreatureRecipe.class,"BLACKLIST")).isEmpty(),"reload releases all blacklist recipe references");
  }finally{watching=false;world.func_72848_b(audio);MinecraftForge.EVENT_BUS.unregister(this);c.base.script("");}
 }
}
