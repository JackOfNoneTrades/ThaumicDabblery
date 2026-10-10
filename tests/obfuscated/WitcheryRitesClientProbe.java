package tdtest;
import java.lang.reflect.Field;
import java.util.Arrays;
import cpw.mods.fml.common.*;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.ritual.RiteRegistry;
import com.emoniph.witchery.client.gui.GuiScreenWitchcraftBook;
@Mod(modid="tdritesclient",name="Witchery rites client probe",version="1",dependencies="required-after:thaumicdabblery;required-after:witchery",acceptableRemoteVersions="*")
public final class WitcheryRitesClientProbe {
 private boolean launched,done,passed;private int ticks,renderTicks;private GuiScreenWitchcraftBook gui;
 @Mod.EventHandler public void ready(FMLLoadCompleteEvent e){FMLCommonHandler.instance().bus().register(this);MinecraftForge.EVENT_BUS.register(this);}
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END||done||++ticks<30)return;Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;try{if(!launched){if(!Boolean.FALSE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment")))throw new AssertionError("not obfuscated");launched=true;ticks=0;String[] a=System.getProperty("td.rites.server").split(":");FMLClientHandler.instance().connectToServerAtStartup(a[0],Integer.parseInt(a[1]));}if(ticks>1400)throw new AssertionError("server never completed checks");if(passed&&gui==null){RiteRegistry.Ritual r=RiteRegistry.instance().getRitual((byte)3);if(!Arrays.equals(r.getCircles(),new byte[]{6}))throw new AssertionError("client circles differ");String text=r.getDescription();if(!text.contains("Diamond")||!text.contains("Cookie")||!text.contains("Pig")||!text.contains("25"))throw new AssertionError("client requirements differ: "+text);
 gui=new GuiScreenWitchcraftBook(mc.field_71439_g,Witchery.Items.GENERIC.itemBookCircleMagic.createStack());NBTTagList pages=(NBTTagList)field("bookPages").get(gui);int page=-1;for(int i=0;i<pages.func_74745_c();i++)if(pages.func_150305_b(i).func_74779_i("Summary").equals(text)){page=i;if(!Arrays.equals(pages.func_150305_b(i).func_74770_j("Circles"),new byte[]{6}))throw new AssertionError("book diagram mismatch");break;}if(page<0)throw new AssertionError("edited rite not in book");mc.func_147108_a(gui);field("currPage").setInt(gui,page);}
 }catch(Throwable t){finish(t);}}
 @SubscribeEvent public void render(TickEvent.RenderTickEvent e){if(e.phase!=TickEvent.Phase.END||gui==null||done||++renderTicks<30)return;try{Minecraft mc=Minecraft.func_71410_x();ScreenShotHelper.func_148259_a(new java.io.File("."),"witchery-rites.png",mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());finish(null);}catch(Throwable t){finish(t);}}
 @SubscribeEvent public void chat(ClientChatReceivedEvent e){if(e.message.func_150260_c().equals("TD_RITES_NETWORK_PASS"))passed=true;}
 private Field field(String name)throws Exception{Field f=GuiScreenWitchcraftBook.class.getDeclaredField(name);f.setAccessible(true);return f;}
 private void finish(Throwable t){done=true;System.out.println(t==null?"TD_RITES_CLIENT_PASS":"TD_RITES_CLIENT_FAILED");if(t!=null)t.printStackTrace();Minecraft.func_71410_x().func_71400_g();}
}
