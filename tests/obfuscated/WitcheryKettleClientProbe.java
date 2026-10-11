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
import com.emoniph.witchery.integration.NEIKettleRecipeHandler;
import codechicken.nei.*;
import codechicken.nei.recipe.*;
import org.fentanylsolutions.thaumicdabblery.feature.witcherykettle.*;
@Mod(modid="tdkettleclient",name="Witchery kettle client probe",version="1",dependencies="required-after:thaumicdabblery;required-after:witchery",acceptableRemoteVersions="*")
public final class WitcheryKettleClientProbe {
 private boolean launched,done,passed,opened,book;private int ticks,renderTicks,checks;
 private void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 private ItemStack stack(String n){return new ItemStack(GameRegistry.findItem("minecraft",n));}
 @Mod.EventHandler public void ready(FMLLoadCompleteEvent e){FMLCommonHandler.instance().bus().register(this);MinecraftForge.EVENT_BUS.register(this);}
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END||done||++ticks<30)return;Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;try{
  if(!launched){check(Boolean.FALSE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment")),"obfuscated client");launched=true;ticks=0;String[] a=System.getProperty("td.kettle.server").split(":");FMLClientHandler.instance().connectToServerAtStartup(a[0],Integer.parseInt(a[1]));}
  if(ticks>1400)throw new AssertionError("server checks timed out");
  if(passed&&!opened){
   check(KettleRegistry.recipes().size()==40,"server definitions synchronized");
   NEIKettleRecipeHandler all=new NEIKettleRecipeHandler();all.loadCraftingRecipes("witchery_brewing");
   check(all.arecipes.size()==40,"native and custom recipes displayed, removed recipe hidden");
   int diamond=-1;
   for(int i=0;i<all.arecipes.size();i++){
    check(all.arecipes.get(i) instanceof KettleNEI.Cached,"custom recipe cache");
    KettleNEI.Cached c=(KettleNEI.Cached)all.arecipes.get(i);
    check(c.getIngredients().size()==6,"six actual ingredients, output not an ingredient");
    KettleRegistry.Recipe r=KettleRegistry.definition(c.recipe);
    if(r.id.equals("custom:diamond")){
     diamond=i;check(c.recipe.power==100,"recipe-specific power");
     check(c.getResult().item.field_77994_a==2&&c.getResult().item.func_77978_p().func_74762_e("kettle")==1,"output quantity and NBT");
    }
   }
   check(diamond>=0,"custom recipe present");
   List<String> tooltip=all.handleItemTooltip(null,stack("diamond"),new ArrayList<String>(),diamond);
   check(tooltip.toString().contains("glass bottles")&&tooltip.toString().contains("100")&&tooltip.toString().contains("Dimension: 0"),"collection, cost, dimension tooltip");
   NEIKettleRecipeHandler usage=new NEIKettleRecipeHandler();usage.loadUsageRecipes(stack("cookie"));check(usage.arecipes.size()==3,"ingredient usage includes two custom recipes and native Brew of Sleeping");
   usage=new NEIKettleRecipeHandler();usage.loadUsageRecipes(stack("diamond"));for(TemplateRecipeHandler.CachedRecipe r:usage.arecipes)check(!KettleRegistry.definition(((KettleNEI.Cached)r).recipe).id.startsWith("custom:"),"output not treated as input");
   NEIKettleRecipeHandler out=new NEIKettleRecipeHandler();out.loadCraftingRecipes(stack("diamond"));check(out.arecipes.size()==2,"all output alternatives found");
   check(out.handleItemTooltip(null,stack("diamond"),new ArrayList<String>(),0).toString().contains("cat familiar"),"other alternative's familiar rule");
   GuiCraftingRecipe.openRecipeGui("item",stack("diamond"));
   GuiRecipe<?> gui=(GuiRecipe<?>)mc.field_71462_r;int which=-1;for(int i=0;i<gui.currenthandlers.size();i++)if(gui.currenthandlers.get(i) instanceof NEIKettleRecipeHandler)which=i;
   check(which>=0,"actual NEI search registers kettle");
   java.lang.reflect.Method set=GuiRecipe.class.getDeclaredMethod("setRecipePage",int.class);set.setAccessible(true);set.invoke(gui,which);opened=true;
  }
 }catch(Throwable t){finish(t);}}
 @SubscribeEvent public void render(TickEvent.RenderTickEvent e){if(e.phase!=TickEvent.Phase.END||!opened||done||++renderTicks<40)return;try{Minecraft mc=Minecraft.func_71410_x();
  if(!book){check(mc.field_71462_r instanceof GuiRecipe,"NEI visible");ScreenShotHelper.func_148259_a(new java.io.File("."),"witchery-kettle-nei.png",mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());
   GuiScreenWitchcraftBook gui=new GuiScreenWitchcraftBook(mc.field_71439_g,Witchery.Items.GENERIC.itemBookInfusions.createStack());
   java.lang.reflect.Field pages=GuiScreenWitchcraftBook.class.getDeclaredField("bookPages");pages.setAccessible(true);NBTTagList list=(NBTTagList)pages.get(gui);check(list.func_74745_c()==34,"native book honors removal and hidden custom recipe");check(list.func_150305_b(1).func_74779_i("Summary").contains("Dimension: 0"),"new recipe and requirements in native book");
   java.lang.reflect.Field page=GuiScreenWitchcraftBook.class.getDeclaredField("currPage");page.setAccessible(true);page.setInt(gui,1);mc.func_147108_a(gui);book=true;renderTicks=0;
  }else{check(mc.field_71462_r instanceof GuiScreenWitchcraftBook,"native book visible");ScreenShotHelper.func_148259_a(new java.io.File("."),"witchery-kettle-book.png",mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());finish(null);}
 }catch(Throwable t){finish(t);}}
 @SubscribeEvent public void chat(ClientChatReceivedEvent e){if(e.message.func_150260_c().equals("TD_KETTLE_NETWORK_PASS"))passed=true;}
 private void finish(Throwable t){done=true;System.out.println((t==null?"TD_KETTLE_CLIENT_PASS":"TD_KETTLE_CLIENT_FAILED")+" checks="+checks);if(t!=null)t.printStackTrace();Minecraft.func_71410_x().func_71400_g();}
}
