package tdtest;
import java.util.*;
import cpw.mods.fml.common.*;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.client.gui.GuiScreenWitchcraftBook;
import com.emoniph.witchery.integration.NEISpinningWheelRecipeHandler;
import com.emoniph.witchery.crafting.BrazierRecipes;
import com.emoniph.witchery.crafting.SpinningRecipes;
import codechicken.nei.recipe.*;
import org.fentanylsolutions.thaumicdabblery.feature.witcheryspinningwheel.*;
import org.fentanylsolutions.thaumicdabblery.feature.witcherybrazier.*;
@Mod(modid="tdspinningbrazierclient",name="Witchery spinning and brazier client probe",version="1",dependencies="required-after:thaumicdabblery;required-after:witchery",acceptableRemoteVersions="*")
public final class WitcherySpinningBrazierClientProbe {
 private boolean launched,done,passed,opened,book;private int ticks,renderTicks,checks;
 private void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 private ItemStack stack(String n){return new ItemStack(GameRegistry.findItem("minecraft",n));}
 @Mod.EventHandler public void ready(FMLLoadCompleteEvent e){FMLCommonHandler.instance().bus().register(this);MinecraftForge.EVENT_BUS.register(this);}
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END||done||++ticks<30)return;Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;try{
  if(!launched){check(Boolean.FALSE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment")),"obfuscated client");launched=true;ticks=0;String[] a=System.getProperty("td.spinningbrazier.server").split(":");FMLClientHandler.instance().connectToServerAtStartup(a[0],Integer.parseInt(a[1]));}
  if(ticks>1400)throw new AssertionError("server checks timed out");
  if(passed&&!opened){
   check(SpinningRegistry.recipes().size()==6&&BrazierRegistry.recipes().size()==10,"both registries synchronize without local scripts");
   for(SpinningRegistry.Recipe r:SpinningRegistry.recipes())check(r.id.startsWith("builtin:")||r.id.startsWith("custom:"),"stable spinning prefix");
   for(BrazierRegistry.Recipe r:BrazierRegistry.recipes())check(r.id.startsWith("builtin:")||r.id.startsWith("custom:"),"stable brazier prefix");
   NEISpinningWheelRecipeHandler all=new NEISpinningWheelRecipeHandler();all.loadCraftingRecipes("witchery_spinning");check(all.arecipes.size()==6,"all builtin and custom spinning recipes shown");int thread=-1;
   for(int i=0;i<all.arecipes.size();i++){
    check(all.arecipes.get(i) instanceof SpinningNEI.Cached,"corrected recipe cache");SpinningNEI.Cached c=(SpinningNEI.Cached)all.arecipes.get(i);
    if(c.getResult().item.func_77973_b()==stack("string").func_77973_b()){thread=i;check(c.getIngredients().size()==3,"fibre plus two modifiers only");check(c.getIngredients().get(0).item.field_77994_a==2,"counted fibre displayed");check(c.getResult().item.field_77994_a==4&&c.getResult().item.func_77978_p().func_74762_e("spinning")==1,"counted NBT output displayed");}
   }
   check(thread>=0,"custom thread present");List<String> tooltip=all.handleItemTooltip(null,stack("string"),new ArrayList<String>(),thread);check(tooltip.toString().contains("180")&&tooltip.toString().contains("15 seconds"),"native timing and power tooltip");
   NEISpinningWheelRecipeHandler usage=new NEISpinningWheelRecipeHandler();usage.loadUsageRecipes(stack("string"));int expected=0;for(SpinningRecipes.SpinningRecipe r:SpinningRecipes.instance().recipes)if(r.uses(stack("string")))expected++;check(usage.arecipes.size()==expected&&expected>1,"all native ingredient usages returned");
   usage=new NEISpinningWheelRecipeHandler();usage.loadUsageRecipes(stack("diamond"));check(usage.arecipes.isEmpty(),"result excluded from ingredient usages");
   for(BrazierRecipes.BrazierRecipe r:BrazierRecipes.instance().recipes){BrazierRegistry.Recipe d=BrazierRegistry.definition(r);if(d.id.equals("custom:guardian"))check(d.entity.equals("Pig")&&d.nbt.func_74779_i("CustomName").equals("Crypt Guardian")&&d.power==.5F,"summoning NBT and power synchronized");if(d.id.equals("custom:war_incense"))check(!r.getNeedsPower()&&d.effect==BrazierRecipes.STRONG,"copied aura retains native client effect identity");}
   GuiCraftingRecipe.openRecipeGui("item",stack("string"));GuiRecipe<?> gui=(GuiRecipe<?>)mc.field_71462_r;int which=-1;for(int i=0;i<gui.currenthandlers.size();i++)if(gui.currenthandlers.get(i) instanceof NEISpinningWheelRecipeHandler)which=i;check(which>=0,"actual NEI search exposes spinning");java.lang.reflect.Method set=GuiRecipe.class.getDeclaredMethod("setRecipePage",int.class);set.setAccessible(true);set.invoke(gui,which);opened=true;
  }
 }catch(Throwable t){finish(t);}}
 @SubscribeEvent public void render(TickEvent.RenderTickEvent e){if(e.phase!=TickEvent.Phase.END||!opened||done||++renderTicks<40)return;try{Minecraft mc=Minecraft.func_71410_x();
  if(!book){check(mc.field_71462_r instanceof GuiRecipe,"NEI visible");ScreenShotHelper.func_148259_a(new java.io.File("."),"witchery-spinning-nei.png",mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());GuiScreenWitchcraftBook gui=new GuiScreenWitchcraftBook(mc.field_71439_g,Witchery.Items.GENERIC.itemBookBurning.createStack());java.lang.reflect.Field pages=GuiScreenWitchcraftBook.class.getDeclaredField("bookPages");pages.setAccessible(true);NBTTagList list=(NBTTagList)pages.get(gui);int guardian=-1,war=-1;for(int i=0;i<list.func_74745_c();i++){String text=list.func_150305_b(i).func_74779_i("Summary");if(text.contains("Crypt Guardian"))guardian=i;if(text.contains("War Incense"))war=i;}check(guardian>=0&&war>=0,"native burning book lists both custom effects");String text=list.func_150305_b(guardian).func_74779_i("Summary");check(text.contains("0.3 seconds")&&text.contains("0.5 per working tick")&&text.contains("Diamond"),"book shows edited duration power and ingredients");java.lang.reflect.Field page=GuiScreenWitchcraftBook.class.getDeclaredField("currPage");page.setAccessible(true);page.setInt(gui,guardian);mc.func_147108_a(gui);book=true;renderTicks=0;
  }else{check(mc.field_71462_r instanceof GuiScreenWitchcraftBook,"native book visible");ScreenShotHelper.func_148259_a(new java.io.File("."),"witchery-brazier-book.png",mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());finish(null);}
 }catch(Throwable t){finish(t);}}
 @SubscribeEvent public void chat(ClientChatReceivedEvent e){if(e.message.func_150260_c().equals("TD_SPINNING_BRAZIER_NETWORK_PASS"))passed=true;}
 private void finish(Throwable t){done=true;System.out.println((t==null?"TD_SPINNING_BRAZIER_CLIENT_PASS":"TD_SPINNING_BRAZIER_CLIENT_FAILED")+" checks="+checks);if(t!=null)t.printStackTrace();Minecraft.func_71410_x().func_71400_g();}
}
