package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
@Mod(modid="tdwarpclient",name="Warp event client probe",version="1",dependencies="required-after:thaumicdabblery",acceptableRemoteVersions="*")
public final class WarpEventClientProbe {
 private boolean launched,done,explicit;private int ticks,messages;
 @Mod.EventHandler public void ready(FMLLoadCompleteEvent e){FMLCommonHandler.instance().bus().register(this);MinecraftForge.EVENT_BUS.register(this);}
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END||done||++ticks<30)return;Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;try{if(!launched){launched=true;ticks=0;String[] a=System.getProperty("td.warp.server").split(":");FMLClientHandler.instance().connectToServerAtStartup(a[0],Integer.parseInt(a[1]));}if(ticks>1200)throw new AssertionError("server never completed warp checks");}catch(Throwable t){finish(t);}}
 @SubscribeEvent public void chat(ClientChatReceivedEvent e){if(done)return;String text=e.message.func_150260_c();try{if(text.equals("Warp message probe")){messages++;if(e.message.func_150256_b().func_150215_a()!=net.minecraft.util.EnumChatFormatting.DARK_PURPLE||!e.message.func_150256_b().func_150242_c())throw new AssertionError("wrong warp message style");}if(text.contains("Warp explicit message"))explicit=true;if(text.contains("Warp:manual gave")||text.contains("[Warp:")||text.contains("Given Weakness")||text.contains("Played sound"))throw new AssertionError("routine warp feedback leaked: "+text);if(text.equals("TD_WARP_NETWORK_PASS")){if(messages!=1)throw new AssertionError("expected exactly one optional message, got "+messages);if(!explicit)throw new AssertionError("explicit tellraw not delivered");finish(null);}}catch(Throwable t){finish(t);}}
 private void finish(Throwable t){done=true;System.out.println(t==null?"TD_WARP_CLIENT_PASS":"TD_WARP_CLIENT_FAILED");if(t!=null)t.printStackTrace();Minecraft.func_71410_x().func_71400_g();}
}
