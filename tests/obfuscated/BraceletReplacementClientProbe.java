package tdtest;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import codechicken.nei.recipe.TemplateRecipeHandler;
import com.gtnewhorizons.aspectrecipeindex.util.ARIConfig;
import dev.rndmorris.salisarcana.common.compat.nei.WandCapSubstitutionHandler;
import dev.rndmorris.salisarcana.common.compat.nei.WandCoreSubstitutionHandler;
import minetweaker.MineTweakerImplementationAPI;

@Mod(modid="tdbraceletclientprobe", version="1", dependencies="required-after:thaumicdabblery")
public final class BraceletReplacementClientProbe {
    private int ticks;
    private boolean launched, done;
    @Mod.EventHandler public void complete(FMLLoadCompleteEvent event) { FMLCommonHandler.instance().bus().register(this); }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END || ++ticks < 30) return;
        Minecraft mc = Minecraft.func_71410_x();
        BraceletReplacementChecks checks = new BraceletReplacementChecks();
        try {
            if (!launched) {
                launched = true; ticks = 0;
                mc.func_71371_a("td_bracelet_replacement", "td bracelet replacement", new WorldSettings(42L, WorldSettings.GameType.CREATIVE, false, false, WorldType.field_77138_c).func_77166_b());
                return;
            }
            if (mc.field_71439_g == null) { checks.check(ticks < 2400, "world joins"); return; }
            done = true;
            checks.run(mc.field_71439_g);
            if (Loader.isModLoaded("aspectrecipeindex")) checkNei(checks);
            else MineTweakerImplementationAPI.reload();
            System.out.println("TD_BRACELET_CLIENT_PASS checks=" + checks.checks);
        } catch (Throwable failure) {
            done = true;
            System.out.println("TD_BRACELET_CLIENT_FAILED checks=" + checks.checks); failure.printStackTrace();
        }
        if (done) mc.func_71400_g();
    }
    private static void checkNei(BraceletReplacementChecks checks) throws Exception {
        ARIConfig.showLockedRecipes = true;
        for (int reload = 0; reload < 2; reload++) {
            for (TemplateRecipeHandler handler : new TemplateRecipeHandler[]{new WandCapSubstitutionHandler(), new WandCoreSubstitutionHandler()}) {
                for (ItemStack bracelet : checks.bracelets()) {
                    handler.arecipes.clear();
                    ItemStack before = bracelet.func_77946_l();
                    handler.loadUsageRecipes(bracelet);
                    checks.check(handler.arecipes.isEmpty(), "NEI hides all bracelet variants");
                    checks.check(ItemStack.func_77989_b(before, bracelet), "NEI leaves bracelet unchanged");
                }
                for (boolean staff : new boolean[]{false, true}) for (boolean sceptre : new boolean[]{false, true}) {
                    handler.arecipes.clear();
                    handler.loadUsageRecipes(checks.wand(staff, sceptre));
                    checks.check(!handler.arecipes.isEmpty(), "NEI retains ordinary replacement recipes");
                }
            }
            MineTweakerImplementationAPI.reload();
        }
    }
}
