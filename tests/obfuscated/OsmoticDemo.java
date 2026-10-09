package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.world.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.block.Block;
import net.minecraft.command.*;
import thaumic.tinkerer.common.block.tile.TileEnchanter;
@Mod(modid="tdosmoticdemo",name="Osmotic demo",version="1",acceptableRemoteVersions="*",dependencies="required-after:thaumicdabblery;required-after:ThaumicTinkerer;required-after:modtweaker2")
public final class OsmoticDemo {
 private int loginTicks;private boolean opened;
 public static String script(){String s=OsmoticChecks.PREFIX+OsmoticChecks.definition(16,"ordo 10",OsmoticChecks.RESEARCH);for(int id:OsmoticChecks.custom.subList(0,18))s+=OsmoticChecks.definition(id,"aer 5","");return s;}
 @Mod.EventHandler public void init(FMLPostInitializationEvent e){OsmoticChecks.register();FMLCommonHandler.instance().bus().register(this);}
 @Mod.EventHandler public void starting(FMLServerStartingEvent e){e.registerServerCommand(new CommandBase(){public int compareTo(Object o){return 0;}public String func_71517_b(){return "tdosmotic";}public String func_71518_a(ICommandSender s){return "/tdosmotic remove <id>";}public void func_71515_b(ICommandSender sender,String[] args){try{if(args[0].equals("empty")||args[0].equals("fill")){TileEnchanter tile=(TileEnchanter)MinecraftServer.func_71276_C().func_71218_a(0).func_147438_o(0,4,0);net.minecraft.item.ItemStack wand=tile.func_70301_a(1);for(thaumcraft.api.aspects.Aspect aspect:new thaumcraft.api.aspects.Aspect[]{thaumcraft.api.aspects.Aspect.AIR,thaumcraft.api.aspects.Aspect.EARTH,thaumcraft.api.aspects.Aspect.FIRE,thaumcraft.api.aspects.Aspect.WATER,thaumcraft.api.aspects.Aspect.ORDER,thaumcraft.api.aspects.Aspect.ENTROPY})((thaumcraft.common.items.wands.ItemWandCasting)wand.func_77973_b()).storeVis(wand,aspect,args[0].equals("empty")?0:5000);org.fentanylsolutions.thaumicdabblery.feature.osmotic.OsmoticRecipes.sync(tile);return;}int id=Integer.parseInt(args[1]);OsmoticChecks.script(script()+"OsmoticEnchanter.removeEnchantment("+id+");\n");}catch(Exception ex){throw new RuntimeException(ex);}}});}
 @Mod.EventHandler public void started(FMLServerStartedEvent e){try{WorldServer w=MinecraftServer.func_71276_C().func_71218_a(0);OsmoticChecks.script(script());w.func_82736_K().func_82764_b("doMobSpawning","false");for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)w.func_147465_d(x,3,z,net.minecraft.init.Blocks.field_150348_b,0,3);TileEnchanter t=OsmoticChecks.tile(w,0,4,0);System.out.println("TD_OSMOTIC_DEMO_READY");}catch(Throwable t){System.out.println("TD_OSMOTIC_DEMO_FAILED");t.printStackTrace();}}
 @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent e){if(!(e.player instanceof EntityPlayerMP))return;EntityPlayerMP p=(EntityPlayerMP)e.player;MinecraftServer.func_71276_C().func_71203_ab().func_152605_a(p.func_146103_bH());p.func_71033_a(WorldSettings.GameType.SURVIVAL);p.field_71075_bZ.field_75102_a=true;p.func_71016_p();p.field_71135_a.func_147364_a(.5,4,-2,0,0);}
 @SubscribeEvent public void tick(TickEvent.PlayerTickEvent e){if(e.side.isServer()&&e.phase==TickEvent.Phase.END&&!opened&&++loginTicks>=40){opened=true;System.out.println("TD_OSMOTIC_OPEN_GUI");cpw.mods.fml.common.network.internal.FMLNetworkHandler.openGui(e.player,thaumic.tinkerer.common.ThaumicTinkerer.instance,2,e.player.field_70170_p,0,4,0);}}
}
