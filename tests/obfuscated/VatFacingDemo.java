package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.world.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;
import com.kentington.thaumichorizons.common.ThaumicHorizons;
import com.kentington.thaumichorizons.common.tiles.*;
@Mod(modid="tdvatfacingdemo",name="Vat facing demo",version="1",acceptableRemoteVersions="*",dependencies="required-after:thaumicdabblery;required-after:ThaumicHorizons;required-after:modtweaker2")
public final class VatFacingDemo {
 @Mod.EventHandler public void ready(FMLInitializationEvent event){FMLCommonHandler.instance().bus().register(this);}
 @Mod.EventHandler public void started(FMLServerStartedEvent event){try{
  WorldServer w=MinecraftServer.func_71276_C().func_71218_a(0);if(!java.nio.file.Files.exists(java.nio.file.Paths.get("scripts/zz-vat-facing.zs")))VatFacingChecks.script(VatFacingChecks.DEMO);
  w.func_82736_K().func_82764_b("doDaylightCycle","false");w.func_82736_K().func_82764_b("doMobSpawning","false");w.func_72877_b(6000);
  for(int x=-20;x<=20;x++)for(int z=-12;z<=12;z++)w.func_147465_d(x,3,z,(Block)Block.field_149771_c.func_82594_a("stonebrick"),0,3);
  if(!(w.func_147438_o(-7,7,0) instanceof TileVat)){
   TileVat pig=VatFacingChecks.assembled(w,-7,7,0);EntityPig p=new EntityPig(w);p.func_94058_c("Diamond Dial Pig");pig.setEntityContained(p);w.func_147471_g(-7,7,0);
   EntityItemFrame f=VatFacingChecks.frame(w,-8,6,-1,2,new ItemStack(Items.field_151045_i));
   TileVat effigy=VatFacingChecks.assembled(w,0,7,0);effigy.mode=4;w.func_147465_d(0,8,0,ThaumicHorizons.blockSoulBeacon,0,3);w.func_147471_g(0,7,0);
   TileVat zombie=VatFacingChecks.assembled(w,7,7,0);EntityZombie z=new EntityZombie(w);z.func_94058_c("Body Tracking Zombie");zombie.setEntityContained(z);w.func_147471_g(7,7,0);
  }
  sign(w,-7,"PIG","Diamond dial","Right-click","the frame");sign(w,0,"YOUR EFFIGY","Head tracking","60 degree limit","Walk around me");sign(w,7,"ZOMBIE","Body tracking","16 block range","Walk around me");
  System.out.println("TD_VAT_DEMO_READY vats=(-7,7,0),(0,7,0),(7,7,0)");
 }catch(Throwable t){System.out.println("TD_VAT_DEMO_FAILED");t.printStackTrace();}}
 private void sign(WorldServer w,int x,String... lines){w.func_147465_d(x,4,-3,(Block)Block.field_149771_c.func_82594_a("standing_sign"),8,3);TileEntitySign sign=(TileEntitySign)w.func_147438_o(x,4,-3);System.arraycopy(lines,0,sign.field_145915_a,0,4);sign.func_70296_d();w.func_147471_g(x,4,-3);}
 @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent event){if(!(event.player instanceof EntityPlayerMP))return;EntityPlayerMP p=(EntityPlayerMP)event.player;WorldServer w=(WorldServer)p.field_70170_p;
  MinecraftServer.func_71276_C().func_71203_ab().func_152605_a(p.func_146103_bH());p.func_71033_a(WorldSettings.GameType.CREATIVE);p.field_71135_a.func_147364_a(0.5,4,-8,0,0);
  p.field_71071_by.func_70441_a(new ItemStack(Items.field_151045_i,64));p.field_71071_by.func_70441_a(new ItemStack((net.minecraft.item.Item)net.minecraft.item.Item.field_150901_e.func_82594_a("item_frame"),8));
  ((TileSoulBeacon)w.func_147438_o(0,8,0)).activate(p);
  p.func_145747_a(new ChatComponentText("Vat demo: pig on the right, your effigy in the middle, zombie on the left. Rotate the framed diamond with right-click. /tp "+p.func_70005_c_()+" 0.5 4 -8 returns here."));
  System.out.println("TD_VAT_DEMO_PLAYER_READY "+p.func_70005_c_());
 }
}
