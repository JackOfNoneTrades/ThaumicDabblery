package tdtest;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.fentanylsolutions.thaumicdabblery.Config;
import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;
import org.fentanylsolutions.thaumicdabblery.feature.wandcomponents.WandComponentStatsRegistry;
import org.lwjgl.input.Keyboard;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ConfigItems;

/** Client-only test mod. Forces expanded Salis tooltips and exits after checking the actual event pipeline. */
@Mod(modid = "tdwandclientprobe", version = "1", dependencies = "required-after:thaumicdabblery;required-after:salisarcana")
public class WandComponentClientProbe {
    private int ticks;
    private int checks;
    private boolean done;

    @Mod.EventHandler
    public void complete(FMLLoadCompleteEvent event) {
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END || ++ticks < 20) return;
        done = true;
        try {
            check(Boolean.FALSE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment")), "obfuscated client");
            checks += new WandCraftingCostChecks().run(null);
            Field keys = Keyboard.class.getDeclaredField("keyDownBuffer");
            keys.setAccessible(true);
            ByteBuffer state = (ByteBuffer) keys.get(null);
            byte previousKey = state.get(Keyboard.KEY_LCONTROL);
            state.put(Keyboard.KEY_LCONTROL, (byte) 1);
            try {
                configure(false);
                String nativeRune = StatCollector.func_74838_a("salisarcana:wand_rod.runes");
                String nativeRegen = StatCollector.func_74838_a("salisarcana:wand_rod.special.blaze");
                check(tooltip(ConfigItems.STAFF_ROD_PRIMAL.getItem()).contains(nativeRune), "baseline Salis rune description");
                check(tooltip(ConfigItems.WAND_ROD_BLAZE.getItem()).contains(nativeRegen), "baseline Salis regeneration description");
                configure(true);
                WandComponentStatsRegistry.setPotency("primal_staff", 3);
                WandComponentStatsRegistry.setRegeneration("blaze", Aspect.FIRE, 40, 0.5, 20);
                WandComponentStatsRegistry.setCoreCapacity(ConfigItems.WAND_ROD_GREATWOOD, 80);
                WandComponentStatsRegistry.setCoreCost(ConfigItems.WAND_ROD_GREATWOOD, 11);
                WandComponentStatsRegistry.setCapCost(ConfigItems.WAND_CAP_GOLD, 4);

                List<String> primal = tooltip(ConfigItems.STAFF_ROD_PRIMAL.getItem());
                check(!primal.contains(nativeRune), "obsolete +1 rune description removed");
                check(primal.contains(StatCollector.func_74837_a("thaumicdabblery.wand.potency", 3)), "effective +3 Potency shown");
                List<String> blaze = tooltip(ConfigItems.WAND_ROD_BLAZE.getItem());
                check(!blaze.contains(nativeRegen), "obsolete regen description removed");
                check(contains(blaze, "40 ticks"), "custom regeneration interval shown");
                check(contains(blaze, "0.5"), "fractional regeneration amount shown");
                List<String> core = tooltip(ConfigItems.WAND_ROD_GREATWOOD.getItem());
                check(core.contains(StatCollector.func_74837_a("salisarcana:wand_rod.vis_capacity", 80)), "Salis core capacity updated");
                check(core.contains(StatCollector.func_74837_a("salisarcana:wand_rod.base_price", 11)), "Salis core cost updated");
                List<String> cap = tooltip(ConfigItems.WAND_CAP_GOLD.getItem());
                check(cap.contains(StatCollector.func_74837_a("salisarcana:wand_cap.price_mult", 4)), "Salis cap multiplier updated");
                WandComponentStatsRegistry.disableRegeneration("blaze");
                List<String> disabled = tooltip(ConfigItems.WAND_ROD_BLAZE.getItem());
                check(!disabled.contains(nativeRegen), "disabled regeneration removes native text");
                check(disabled.contains(StatCollector.func_74838_a("thaumicdabblery.wand.regeneration.disabled")), "disabled regeneration shown");

                ItemStack assembled = new ItemStack(ConfigItems.itemWandCasting);
                ((thaumcraft.common.items.wands.ItemWandCasting) assembled.func_77973_b()).setRod(assembled, ConfigItems.STAFF_ROD_PRIMAL);
                check(tooltip(assembled).contains(StatCollector.func_74837_a("thaumicdabblery.wand.potency", 3)), "assembled innate bonus shown");
                Runnable zeroPotency = WandComponentStatsRegistry.setPotency("primal_staff", 0);
                try {
                    String zeroLine = StatCollector.func_74837_a("thaumicdabblery.wand.potency", 0);
                    List<String> zeroCore = tooltip(ConfigItems.STAFF_ROD_PRIMAL.getItem());
                    check(!zeroCore.contains(zeroLine), "zero innate Potency hidden on loose core");
                    check(!zeroCore.contains(nativeRune), "zero override removes Salis native +1");
                    List<String> zeroWand = tooltip(assembled);
                    check(!zeroWand.contains(zeroLine), "zero innate Potency hidden on assembled wand");
                    check(!zeroWand.contains(nativeRune), "zero assembled override has no native +1");
                    check(contains(zeroCore, "32"), "other core tooltip information retained");
                } finally {
                    zeroPotency.run();
                }
                check(tooltip(ConfigItems.STAFF_ROD_PRIMAL.getItem()).contains(StatCollector.func_74837_a("thaumicdabblery.wand.potency", 3)), "undo zero restores positive core tooltip");
                check(tooltip(assembled).contains(StatCollector.func_74837_a("thaumicdabblery.wand.potency", 3)), "undo zero restores positive assembled tooltip");
                System.out.println("TD_WAND_CLIENT_ALL_PASS checks=" + checks);
            } finally {
                state.put(Keyboard.KEY_LCONTROL, previousKey);
                configure(true);
            }
        } catch (Throwable failure) {
            System.out.println("TD_WAND_CLIENT_FAILED checks=" + checks);
            failure.printStackTrace();
        } finally {
            Minecraft.func_71410_x().func_71400_g();
        }
    }

    private List<String> tooltip(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        MinecraftForge.EVENT_BUS.post(new ItemTooltipEvent(stack.func_77946_l(), null, lines, false));
        System.out.println("TD_TOOLTIP " + lines);
        return lines;
    }

    private boolean contains(List<String> lines, String part) {
        for (String line : lines) if (line.contains(part)) return true;
        return false;
    }

    private void configure(boolean enabled) {
        Config.getRawConfig().get("features.wandComponentStats", "enabled", true).set(enabled);
        Config.getRawConfig().save();
        ThaumicDabblery.proxy.onConfigReload();
    }

    private void check(boolean result, String description) {
        if (!result) throw new AssertionError(description);
        checks++;
    }
}
