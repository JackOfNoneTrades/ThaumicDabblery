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
import com.emoniph.witchery.integration.NEIDistilleryRecipeHandler;
import codechicken.nei.recipe.*;
import org.fentanylsolutions.thaumicdabblery.feature.witcherydistillery.*;
@Mod(modid="tddistilleryclient",name="Witchery distillery client probe",version="1",dependencies="required-after:thaumicdabblery;required-after:witchery",acceptableRemoteVersions="*")
public final class WitcheryDistilleryClientProbe {
 private boolean launched,done,passed,opened,book;private int ticks,renderTicks,checks;
 private void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 private ItemStack stack(String n){return new ItemStack(GameRegistry.findItem("minecraft",n));}
 @Mod.EventHandler public void ready(FMLLoadCompleteEvent e){FMLCommonHandler.instance().bus().register(this);MinecraftForge.EVENT_BUS.register(this);}
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END||done||++ticks<30)return;Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;try{
  if(!launched){check(Boolean.FALSE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment")),"obfuscated client");launched=true;ticks=0;String[] a=System.getProperty("td.distillery.server").split(":");FMLClientHandler.instance().connectToServerAtStartup(a[0],Integer.parseInt(a[1]));}
  if(ticks>1400)throw new AssertionError("server checks timed out");
  if(passed&&!opened){
   check(DistilleryRegistry.recipes().size()==12,"server definitions synchronized");
   NEIDistilleryRecipeHandler all=new NEIDistilleryRecipeHandler();all.loadCraftingRecipes("witchery_distilling");check(all.arecipes.size()==12,"all native and custom recipes shown");int diamond=-1;
   for(int i=0;i<all.arecipes.size();i++){
    check(all.arecipes.get(i) instanceof DistilleryNEI.Cached,"custom recipe cache");DistilleryNEI.Cached c=(DistilleryNEI.Cached)all.arecipes.get(i);DistilleryRegistry.Recipe r=DistilleryRegistry.definition(c.recipe);
    if(r.id.equals("custom:diamond")){diamond=i;check(c.recipe.jars==2,"per-recipe jars");check(c.getIngredients().size()==3&&c.getIngredients().get(2).item.field_77994_a==2,"two inputs plus jar cost");check(c.getOtherStacks().size()==3,"secondary outputs exposed separately from ingredients");check(c.getResult().item.field_77994_a==2&&c.getResult().item.func_77978_p().func_74762_e("distillery")==1,"count and output NBT");}
   }
   check(diamond>=0,"custom recipe present");List<String> tooltip=all.handleItemTooltip(null,stack("diamond"),new ArrayList<String>(),diamond);check(tooltip.toString().contains("Empty clay jars: 2")&&tooltip.toString().contains("480")&&tooltip.toString().contains("40 seconds"),"cost and timing tooltip");
   NEIDistilleryRecipeHandler usage=new NEIDistilleryRecipeHandler();usage.loadUsageRecipes(stack("cookie"));check(usage.arecipes.size()==2,"all custom ingredient usages");usage=new NEIDistilleryRecipeHandler();usage.loadUsageRecipes(stack("emerald"));check(usage.arecipes.isEmpty(),"secondary outputs not treated as ingredients");
   usage=new NEIDistilleryRecipeHandler();usage.loadUsageRecipes(Witchery.Items.GENERIC.itemEmptyClayJar.createStack());check(usage.arecipes.size()==11,"jar usages omit zero-jar recipe");
   NEIDistilleryRecipeHandler out=new NEIDistilleryRecipeHandler();out.loadCraftingRecipes(stack("emerald"));check(out.arecipes.size()==2,"primary and secondary output lookup finds both recipes");
   GuiCraftingRecipe.openRecipeGui("item",stack("diamond"));GuiRecipe<?> gui=(GuiRecipe<?>)mc.field_71462_r;int which=-1;for(int i=0;i<gui.currenthandlers.size();i++)if(gui.currenthandlers.get(i) instanceof NEIDistilleryRecipeHandler)which=i;check(which>=0,"actual NEI search registers distillery");java.lang.reflect.Method set=GuiRecipe.class.getDeclaredMethod("setRecipePage",int.class);set.setAccessible(true);set.invoke(gui,which);opened=true;
  }
 }catch(Throwable t){finish(t);}}
 @SubscribeEvent public void render(TickEvent.RenderTickEvent e){if(e.phase!=TickEvent.Phase.END||!opened||done||++renderTicks<40)return;try{Minecraft mc=Minecraft.func_71410_x();
  if(!book){check(mc.field_71462_r instanceof GuiRecipe,"NEI visible");ScreenShotHelper.func_148259_a(new java.io.File("."),"witchery-distillery-nei.png",mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());GuiScreenWitchcraftBook gui=new GuiScreenWitchcraftBook(mc.field_71439_g,Witchery.Items.GENERIC.itemBookDistilling.createStack());java.lang.reflect.Field pages=GuiScreenWitchcraftBook.class.getDeclaredField("bookPages");pages.setAccessible(true);NBTTagList list=(NBTTagList)pages.get(gui);check(list.func_74745_c()==13,"native book includes all recipes");check(list.func_150305_b(3).func_74779_i("Summary").contains("2 x Diamond")&&list.func_150305_b(3).func_74779_i("Summary").contains("4 x Redstone"),"native book shows output counts");java.lang.reflect.Field page=GuiScreenWitchcraftBook.class.getDeclaredField("currPage");page.setAccessible(true);page.setInt(gui,3);mc.func_147108_a(gui);book=true;renderTicks=0;
  }else{check(mc.field_71462_r instanceof GuiScreenWitchcraftBook,"native book visible");ScreenShotHelper.func_148259_a(new java.io.File("."),"witchery-distillery-book.png",mc.field_71443_c,mc.field_71440_d,mc.func_147110_a());finish(null);}
 }catch(Throwable t){finish(t);}}
 @SubscribeEvent public void chat(ClientChatReceivedEvent e){if(e.message.func_150260_c().equals("TD_DISTILLERY_NETWORK_PASS"))passed=true;}
 private void finish(Throwable t){done=true;System.out.println((t==null?"TD_DISTILLERY_CLIENT_PASS":"TD_DISTILLERY_CLIENT_FAILED")+" checks="+checks);if(t!=null)t.printStackTrace();Minecraft.func_71410_x().func_71400_g();}
}
