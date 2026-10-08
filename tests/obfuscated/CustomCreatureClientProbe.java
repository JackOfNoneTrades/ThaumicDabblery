package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.*;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import com.kentington.thaumichorizons.common.ThaumicHorizons;
import com.kentington.thaumichorizons.common.tiles.TileVat;
import thaumcraft.api.research.*;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TilePedestal;
import thaumcraft.client.gui.GuiResearchRecipe;
@Mod(modid="tdcustomcreatureclientprobe",name="Custom creature client probe",version="1",dependencies="required-after:thaumicdabblery;required-after:modtweaker2;required-after:ThaumicHorizons;after:tc4tweak;after:salisarcana")
public final class CustomCreatureClientProbe {
 private final CustomCreatureChecks checks=new CustomCreatureChecks();
 private int ticks,frames,stage;private boolean launched,done;private volatile boolean serverDone,requestVat,vatDone;private volatile Throwable failure;private int x,y,z;private volatile boolean requestBreach,breachDone;private volatile int breachId;private boolean glassSound,waterSound;
 @Mod.EventHandler public void ready(FMLLoadCompleteEvent e){FMLCommonHandler.instance().bus().register(this);net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);}
 @Mod.EventHandler public void started(FMLServerStartedEvent e){try{checks.run(FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0));}catch(Throwable t){failure=t;}finally{serverDone=true;}}
 @SubscribeEvent public void sound(net.minecraftforge.client.event.sound.PlaySoundEvent17 event){
  if(requestBreach){if(event.name.endsWith("dig.glass"))glassSound=true;if(event.name.endsWith("liquid.water"))waterSound=true;}
  event.result=null; // Observe real received sound events without playing audio during automation.
 }
 private void open(Minecraft mc,int page){mc.func_147108_a(new GuiResearchRecipe(ResearchCategories.getResearch(CustomCreatureChecks.RESEARCH),page,0,0));frames=0;}
 @SubscribeEvent public void serverTick(TickEvent.ServerTickEvent e){
  if(e.phase!=TickEvent.Phase.END||(!requestVat||vatDone)&&(!requestBreach||breachDone))return;
  try{
   MinecraftServer server=MinecraftServer.func_71276_C();if(server.func_71203_ab().field_72404_b.isEmpty())return;
   EntityPlayerMP player=(EntityPlayerMP)server.func_71203_ab().field_72404_b.get(0);WorldServer world=(WorldServer)player.field_70170_p;
   if(requestBreach&&!breachDone){
    CreatureBreachChecks breach=new CreatureBreachChecks(checks,world);breach.x=(int)Math.floor(player.field_70165_t)+12;breach.y=(int)player.field_70163_u+3;breach.z=(int)Math.floor(player.field_70161_v)+3;
    x=breach.x;y=breach.y;z=breach.z;TileVat vat=breach.assembled(breach.subject());breach.start(vat);checks.check(vat.mode==2,"network breach vat starts");breach.finish(vat);
    for(Object o:world.field_72996_f)if(o instanceof EntityPigZombie&&"BreachOutput".equals(((EntityPigZombie)o).func_94057_bL())){EntityPigZombie mob=(EntityPigZombie)o;if(Math.abs(mob.field_70165_t-x)<2)breachId=mob.func_145782_y();}
    checks.check(breachId!=0,"server spawned breach mob");breachDone=true;return;
   }
   x=(int)Math.floor(player.field_70165_t)+2;y=(int)player.field_70163_u+3;z=(int)Math.floor(player.field_70161_v)+2;
   world.func_147465_d(x,y,z,ThaumicHorizons.blockVat,7,3);TileVat vat=(TileVat)world.func_147438_o(x,y,z);
   world.func_147465_d(x+2,y-1,z,ConfigBlocks.blockStoneDevice,1,3);((TilePedestal)world.func_147438_o(x+2,y-1,z)).func_70299_a(0,CustomCreatureChecks.stack("rotten_flesh"));
   EntityPig pig=new EntityPig(world);pig.func_94058_c("Bacon");vat.setEntityContained(pig);ResearchManager.completeResearchUnsaved(player.func_70005_c_(),CustomCreatureChecks.RESEARCH);checks.start(vat,world,player);checks.check(vat.mode==2,"real player's vat starts");checks.complete(vat,world);vatDone=true;
  }catch(Throwable t){failure=t;vatDone=true;}
 }
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END||done||++ticks<30)return;Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;
  try{
   if(!launched){launched=true;ticks=0;mc.func_71371_a("td_custom_creature_"+System.currentTimeMillis(),"Custom creatures",new WorldSettings(42,WorldSettings.GameType.CREATIVE,false,false,WorldType.field_77138_c).func_77166_b());return;}
   if(failure!=null)throw new AssertionError("server checks",failure);if(ticks>1800)throw new AssertionError("client timeout");
   if(!serverDone||mc.field_71439_g==null||ticks<100||(stage>0&&frames<5))return;
   if(stage==0){checks.base.script(CustomCreatureChecks.demo());open(mc,1);requestVat=true;stage++;return;}
   if(stage==1){
    if(!vatDone)return;TileEntity te=mc.field_71441_e.func_147438_o(x,y,z);if(!(te instanceof TileVat)||!(((TileVat)te).getEntityContained() instanceof EntityPigZombie))return;
    checks.check(((TileVat)te).getEntityContained().func_110138_aP()==20,"new mob and species health synchronized to real client");
    checks.check("Bacon".equals(((EntityPigZombie)((TileVat)te).getEntityContained()).func_94057_bL()),"custom name synchronized to client");
   }
   if(stage==4){
    if(!breachDone)return;net.minecraft.entity.Entity mob=mc.field_71441_e.func_73045_a(breachId);if(!(mob instanceof EntityPigZombie))return;
    int drops=0;for(Object o:mc.field_71441_e.field_72996_f)if(o instanceof net.minecraft.entity.item.EntityItem){net.minecraft.entity.item.EntityItem item=(net.minecraft.entity.item.EntityItem)o;if(Math.abs(item.field_70165_t-x)<3&&Math.abs(item.field_70161_v-z)<3&&item.func_92059_d().func_77973_b()==net.minecraft.item.Item.func_150898_a(ThaumicHorizons.blockModifiedMatrix))drops+=item.func_92059_d().field_77994_a;}
    if(!glassSound||!waterSound||drops==0)return;
    checks.check(glassSound&&waterSound,"glass and water sounds arrive through real client sound events");
    checks.check(drops==1&&mc.field_71441_e.func_147437_c(x,y+1,z),"matrix removal and single surviving item arrive at client");
    checks.check(((EntityPigZombie)mob).func_70631_g_()&&"BreachOutput".equals(((EntityPigZombie)mob).func_94057_bL()),"breach output name and baby state reach client");
    checks.check(!(mc.field_71441_e.func_147438_o(x,y,z) instanceof TileVat)&&mc.field_71441_e.func_147437_c(x,y-1,z)&&mc.field_71441_e.func_147437_c(x,y-2,z),"dismantled vat and cleared water reach client");
    checks.base.script("");System.out.println("TD_CUSTOM_CREATURE_CLIENT_PASS checks="+checks.checks);done=true;mc.func_71400_g();return;
   }
   checks.check(mc.field_71462_r instanceof GuiResearchRecipe,"native custom recipe page renders");net.minecraft.util.ScreenShotHelper.func_148259_a(new java.io.File("."),"custom-creature-"+stage+".png",mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());
   if(stage==1){checks.base.script(CustomCreatureChecks.demo()+CreatureInfusionChecks.set("custom:pigman",CustomCreatureChecks.RESEARCH,1,"exanimis 3","<minecraft:cookie>"));open(mc,1);stage++;}
   else if(stage==2){checks.base.script(CustomCreatureChecks.demo()+CreatureInfusionChecks.remove("custom:pigman"));open(mc,0);stage++;}
   else{checks.base.script(CreatureBreachChecks.recipe()+CreatureBreachChecks.nbt()+CreatureBreachChecks.breach(2));mc.func_147108_a(null);mc.field_71474_y.func_151439_a(net.minecraft.client.audio.SoundCategory.MASTER,1.0F);requestBreach=true;stage=4;frames=0;}
  }catch(Throwable t){done=true;System.out.println("TD_CUSTOM_CREATURE_CLIENT_FAILED checks="+checks.checks);t.printStackTrace();mc.func_71400_g();}
 }
 @SubscribeEvent public void render(TickEvent.RenderTickEvent e){if(e.phase==TickEvent.Phase.END)frames++;}
}
