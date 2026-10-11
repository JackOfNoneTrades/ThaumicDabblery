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
import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.integration.NEIWitchesOvenRecipeHandler;
import codechicken.nei.*;
import codechicken.nei.recipe.*;
import org.fentanylsolutions.thaumicdabblery.feature.witcheryoven.*;
@Mod(modid="tdovenclient",name="Witchery oven client probe",version="1",dependencies="required-after:thaumicdabblery;required-after:witchery",acceptableRemoteVersions="*")
public final class WitcheryOvenClientProbe {
 private boolean launched,done,passed,opened;private int ticks,renderTicks,checks;
 private void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 private ItemStack stack(String name,int meta){return new ItemStack(GameRegistry.findItem("minecraft",name),1,meta);}
 private int customs(TemplateRecipeHandler h){int n=0;for(TemplateRecipeHandler.CachedRecipe c:h.arecipes)if(c instanceof OvenNEI.Cached)n++;return n;}
 @Mod.EventHandler public void ready(FMLLoadCompleteEvent e){FMLCommonHandler.instance().bus().register(this);MinecraftForge.EVENT_BUS.register(this);}
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END||done||++ticks<30)return;Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;try{
  if(!launched){check(Boolean.FALSE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment")),"obfuscated client");launched=true;ticks=0;String[] a=System.getProperty("td.oven.server").split(":");FMLClientHandler.instance().connectToServerAtStartup(a[0],Integer.parseInt(a[1]));}
  if(ticks>1400)throw new AssertionError("server checks timed out");
  if(passed&&!opened){
   check(OvenRecipes.rules().size()==6,"server scripts synchronized");
   NEIWitchesOvenRecipeHandler all=new NEIWitchesOvenRecipeHandler();all.loadCraftingRecipes("witchery_cooking");
   check(customs(all)==5,"all five effective custom recipes shown");
   int dirt=-1,nativeCount=0;
   for(int i=0;i<all.arecipes.size();i++){
    TemplateRecipeHandler.CachedRecipe c=all.arecipes.get(i);
    if(c instanceof OvenNEI.Cached){
     OvenNEI.Cached custom=(OvenNEI.Cached)c;
     for(PositionedStack pos:c.getIngredients())for(ItemStack s:pos.items)check(OvenRecipes.select(s)==custom.recipe,"displayed inputs respect priority");
     if(custom.recipe.id.equals("custom:dirt")){dirt=i;check(c.getIngredients().get(0).item.field_77994_a==2,"NEI ingredient count");check(c.getResult().item.field_77994_a==3,"NEI output count");boolean jars=false,cookies=false;for(PositionedStack pos:c.getOtherStacks()){if(Witchery.Items.GENERIC.itemEmptyClayJar.isMatch(pos.item))jars=pos.item.field_77994_a==2;if(pos.item.func_77973_b()==stack("cookie",0).func_77973_b())cookies=pos.item.field_77994_a==2;}check(jars&&cookies,"NEI jars and byproduct counts");}
    }else{nativeCount++;for(PositionedStack pos:c.getIngredients())for(ItemStack s:pos.items)check(OvenRecipes.select(s)==null,"native overrides hidden");}
   }
   check(nativeCount>0&&dirt>=0,"native recipes and custom recipe coexist");
   List<String> tooltip=all.handleItemTooltip(null,Witchery.Items.GENERIC.itemEmptyClayJar.createStack(),new ArrayList<String>(),dirt);check(tooltip.toString().contains("100.0%")&&tooltip.toString().contains("Waits for jars"),"native tooltip hook");
   NEIWitchesOvenRecipeHandler usage=new NEIWitchesOvenRecipeHandler();usage.loadUsageRecipes(stack("log",1));check(customs(usage)==0,"blocked input has no misleading custom uses");
   usage=new NEIWitchesOvenRecipeHandler();usage.loadUsageRecipes(stack("log",0));check(customs(usage)==1,"overridden ore input shows winning recipe only");
   NEIWitchesOvenRecipeHandler output=new NEIWitchesOvenRecipeHandler();output.loadCraftingRecipes(stack("cookie",0));check(customs(output)==2,"byproduct recipe lookup");
   output=new NEIWitchesOvenRecipeHandler();output.loadCraftingRecipes(stack("diamond",0));check(customs(output)==2,"main output lookup");
   GuiCraftingRecipe.openRecipeGui("item",stack("diamond",0));
   GuiRecipe<?> gui=(GuiRecipe<?>)mc.field_71462_r;int which=-1;for(int i=0;i<gui.currenthandlers.size();i++)if(gui.currenthandlers.get(i) instanceof NEIWitchesOvenRecipeHandler)which=i;
   check(which>=0,"native oven handler registered in actual NEI search");
   java.lang.reflect.Method set=GuiRecipe.class.getDeclaredMethod("setRecipePage",int.class);set.setAccessible(true);
   set.invoke(gui,which);opened=true;
  }
 }catch(Throwable t){finish(t);}}
 @SubscribeEvent public void render(TickEvent.RenderTickEvent e){if(e.phase!=TickEvent.Phase.END||!opened||done||++renderTicks<40)return;try{Minecraft mc=Minecraft.func_71410_x();check(mc.field_71462_r instanceof GuiRecipe,"NEI recipe GUI opened");ScreenShotHelper.func_148259_a(new java.io.File("."),"witchery-oven.png",mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());finish(null);}catch(Throwable t){finish(t);}}
 @SubscribeEvent public void chat(ClientChatReceivedEvent e){if(e.message.func_150260_c().equals("TD_OVEN_NETWORK_PASS"))passed=true;}
 private void finish(Throwable t){done=true;System.out.println((t==null?"TD_OVEN_CLIENT_PASS":"TD_OVEN_CLIENT_FAILED")+" checks="+checks);if(t!=null)t.printStackTrace();Minecraft.func_71410_x().func_71400_g();}
}
