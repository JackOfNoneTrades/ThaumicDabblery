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
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import com.emoniph.witchery.integration.NEICauldronRecipeHandler;
import codechicken.nei.*;
import codechicken.nei.recipe.*;
import org.fentanylsolutions.thaumicdabblery.feature.witcherycauldron.*;
@Mod(modid="tdcauldronclient",name="Witchery cauldron client probe",version="1",dependencies="required-after:thaumicdabblery;required-after:witchery",acceptableRemoteVersions="*")
public final class WitcheryCauldronClientProbe {
 private boolean launched,done,passed,opened;private int ticks,renderTicks,checks;
 private void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 private ItemStack stack(String n){return new ItemStack(GameRegistry.findItem("minecraft",n));}
 @Mod.EventHandler public void ready(FMLLoadCompleteEvent e){FMLCommonHandler.instance().bus().register(this);MinecraftForge.EVENT_BUS.register(this);}
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END||done||++ticks<30)return;Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;try{
  if(!launched){check(Boolean.FALSE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment")),"obfuscated client");launched=true;ticks=0;String[] a=System.getProperty("td.cauldron.server").split(":");FMLClientHandler.instance().connectToServerAtStartup(a[0],Integer.parseInt(a[1]));}
  if(ticks>1400)throw new AssertionError("server checks timed out");
  if(passed&&!opened){
   check(CauldronRecipes.recipes().size()==27,"server definitions synchronized");
   NEICauldronRecipeHandler all=new NEICauldronRecipeHandler();all.loadCraftingRecipes("witchery_brewing_plus");
   check(all.arecipes.size()==27,"native and custom recipes displayed, removed recipe hidden");
   int diamond=-1;
   for(int i=0;i<all.arecipes.size();i++){
    check(all.arecipes.get(i) instanceof CauldronNEI.Cached,"custom recipe cache used consistently");
    CauldronNEI.Cached c=(CauldronNEI.Cached)all.arecipes.get(i);
    check(CauldronRecipes.visible(c.recipe),"displayed recipe actually selected");
    check(c.getIngredients().size()<=6,"native six-slot bound");
    if(c.recipe.id.equals("custom:diamond")){
     diamond=i;check(c.recipe.basePower()==100,"recipe-specific base power");
     check(c.getResult().item.field_77994_a==2&&c.getResult().item.func_77978_p().func_74762_e("cauldron")==1,"output quantity and NBT");
     check(c.getIngredients().get(2).item.func_77973_b()==stack("cookie").func_77973_b(),"trigger last in display");
    }
   }
   check(diamond>=0,"custom recipe present");
   List<String> tooltip=all.handleItemTooltip(null,stack("cookie"),new ArrayList<String>(),diamond);
   check(tooltip.toString().contains("Add last")&&tooltip.toString().contains("100"),"trigger and power tooltip");
   NEICauldronRecipeHandler usage=new NEICauldronRecipeHandler();usage.loadUsageRecipes(stack("beef"));check(usage.arecipes.isEmpty(),"removed native usage hidden");
   usage=new NEICauldronRecipeHandler();usage.loadUsageRecipes(stack("cookie"));check(usage.arecipes.size()==1,"trigger lookup");
   NEICauldronRecipeHandler out=new NEICauldronRecipeHandler();out.loadCraftingRecipes(stack("cookie"));check(out.arecipes.size()==1&&out.arecipes.get(0).getResult().item.field_77994_a==2,"edited native output lookup");
   GuiCraftingRecipe.openRecipeGui("item",stack("diamond"));
   GuiRecipe<?> gui=(GuiRecipe<?>)mc.field_71462_r;int which=-1;for(int i=0;i<gui.currenthandlers.size();i++)if(gui.currenthandlers.get(i) instanceof NEICauldronRecipeHandler)which=i;
   check(which>=0,"actual NEI search registers cauldron");
   java.lang.reflect.Method set=GuiRecipe.class.getDeclaredMethod("setRecipePage",int.class);set.setAccessible(true);set.invoke(gui,which);opened=true;
  }
 }catch(Throwable t){finish(t);}}
 @SubscribeEvent public void render(TickEvent.RenderTickEvent e){if(e.phase!=TickEvent.Phase.END||!opened||done||++renderTicks<40)return;try{Minecraft mc=Minecraft.func_71410_x();check(mc.field_71462_r instanceof GuiRecipe,"recipe screen visible");ScreenShotHelper.func_148259_a(new java.io.File("."),"witchery-cauldron.png",mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());finish(null);}catch(Throwable t){finish(t);}}
 @SubscribeEvent public void chat(ClientChatReceivedEvent e){if(e.message.func_150260_c().equals("TD_CAULDRON_NETWORK_PASS"))passed=true;}
 private void finish(Throwable t){done=true;System.out.println((t==null?"TD_CAULDRON_CLIENT_PASS":"TD_CAULDRON_CLIENT_FAILED")+" checks="+checks);if(t!=null)t.printStackTrace();Minecraft.func_71410_x().func_71400_g();}
}
