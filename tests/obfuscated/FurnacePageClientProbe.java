package tdtest;

import cpw.mods.fml.common.*;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.*;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.research.*;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.common.lib.research.ResearchManager;

@Mod(modid="tdfurnaceclientprobe",name="Furnace page client probe",version="1",dependencies="required-after:thaumicdabblery",acceptableRemoteVersions="*")
public final class FurnacePageClientProbe {
 private FurnacePageChecks checks = new FurnacePageChecks();
 private int ticks,frames; private boolean launched,opened,done,serverDone; private Throwable failure;
 @Mod.EventHandler public void complete(FMLLoadCompleteEvent e) { FMLCommonHandler.instance().bus().register(this); }
 @Mod.EventHandler public void started(FMLServerStartedEvent e) {
  try { checks.run(); } catch(Throwable t) { failure=t; } serverDone=true;
 }
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) {
  if(e.phase!=TickEvent.Phase.END||done||++ticks<30)return;
  Minecraft mc=Minecraft.func_71410_x(); mc.field_71474_y.field_82881_y=false;
  try {
   if(!launched) { launched=true;ticks=0;String remote=System.getProperty("td.furnace.server");
    if(remote!=null) { FMLClientHandler.instance().setupServerList();FMLClientHandler.instance().connectToServer(null,new ServerData("Furnace page test",remote)); }
    else mc.func_71371_a("td_furnace_"+System.currentTimeMillis(),"Furnace pages",new WorldSettings(42,WorldSettings.GameType.CREATIVE,false,false,WorldType.field_77138_c).func_77166_b());
    return;
   }
   if(failure!=null)throw new AssertionError("integrated checks",failure);
   if(ticks>1500)throw new AssertionError("client timeout");
   if(mc.field_71439_g==null||mc.field_71441_e==null||ticks<100)return;
   if(System.getProperty("td.furnace.server")==null&&!serverDone)return;
   if(!opened) {
    checks.baseline();
    ResearchItem research=ResearchCategories.getResearch("TD_BAKED_TREATS");
    checks.check(ResearchManager.isResearchComplete(mc.field_71439_g.func_70005_c_(),research.key),"demo unlocks for real player");
    Object[] ref=ThaumcraftApi.getCraftingRecipeKey(mc.field_71439_g,FurnacePageChecks.stack("cookie"));
    checks.check(ref!=null && "TD_BAKED_TREATS".equals(ref[0]) && ((Integer)ref[1])==1,"real recipe click-through target");
    mc.func_147108_a(new GuiResearchRecipe(research,0,0,0));opened=true;
   }
  } catch(Throwable t) { done=true;System.out.println("TD_FURNACE_CLIENT_FAILED checks="+checks.checks);t.printStackTrace();mc.func_71400_g(); }
 }
 @SubscribeEvent public void render(TickEvent.RenderTickEvent e) {
  if(e.phase!=TickEvent.Phase.END||!opened||done||++frames<5)return;
  Minecraft mc=Minecraft.func_71410_x();
  try {
   checks.check(mc.field_71462_r instanceof GuiResearchRecipe,"native Thaumonomicon page rendered");
   net.minecraft.util.ScreenShotHelper.func_148259_a(new java.io.File("."),"furnace-page.png",mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());
   System.out.println("TD_FURNACE_CLIENT_PASS checks="+checks.checks+" remote="+(System.getProperty("td.furnace.server")!=null));
  } catch(Throwable t) {System.out.println("TD_FURNACE_CLIENT_FAILED");t.printStackTrace();}
  done=true;mc.func_71400_g();
 }
}
