package tdtest;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import minetweaker.MineTweakerImplementationAPI;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.api.item.IItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.fentanylsolutions.thaumicdabblery.Config;
import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.WandComponentsZen;
import org.fentanylsolutions.thaumicdabblery.feature.wandcomponents.WandComponentStatsRegistry;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.IWandRodOnUpdate;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.crafting.ArcaneSceptreRecipe;
import thaumcraft.common.lib.crafting.ArcaneWandRecipe;
import thaumcraft.common.tiles.TileMagicWorkbench;

/** Compile against SRG Minecraft and production mods; never put this probe in a player's instance. */
@Mod(modid = "tdwandstatsprobe", version = "1", dependencies = "required-after:thaumicdabblery;after:modtweaker2;after:thaumicbases;after:ThaumicConcilium")
public class WandComponentStatsProbe {
    private int checks;
    private EntityPlayer player;

    @Mod.EventHandler
    public void started(FMLServerStartedEvent event) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        try {
            check(Boolean.FALSE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment")), "production environment required");
            player = FakePlayerFactory.getMinecraft(server.func_71218_a(0));
            scripted();
            nativeAndOverrides();
            validation();
            toggle();
            for (int reload = 0; reload < 3; reload++) {
                MineTweakerImplementationAPI.reload();
                scripted();
            }
            // Remove scripts reversibly, then exercise MineTweaker's actual rollback and replay.
            File[] scripts = new File("scripts").listFiles((dir, name) -> name.endsWith(".zs"));
            List<File> parked = new ArrayList<>();
            try {
                for (File script : scripts) {
                    Files.move(script.toPath(), new File(script.getPath() + ".parked").toPath());
                    parked.add(script);
                }
                MineTweakerImplementationAPI.reload();
                eq(3, ConfigItems.WAND_CAP_GOLD.getCraftCost(), "cap restored after script removal");
                eq(3, ConfigItems.WAND_ROD_GREATWOOD.getCraftCost(), "core cost restored");
                eq(50, ConfigItems.WAND_ROD_GREATWOOD.getCapacity(), "core capacity restored");
                eq(125, ConfigItems.STAFF_ROD_GREATWOOD.getCapacity(), "staff capacity restored");
                ItemStack greatwood = wand(ConfigItems.WAND_ROD_GREATWOOD, false);
                eq(1, item(greatwood).getFocusPotency(greatwood), "innate potency removed, focus upgrade preserved");
                ItemStack blaze = wand(ConfigItems.WAND_ROD_BLAZE, false);
                tick(blaze, 200);
                eq(100, item(blaze).getVis(blaze, Aspect.FIRE), "native regen restored after script removal");
                eq(0, item(blaze).getVis(blaze, Aspect.AIR), "custom regen removed");
                if (Loader.isModLoaded("thaumicbases")) {
                    ItemStack bracelet = bracelet("thaumicbases", "castingBracelet", 0);
                    eq(1000, item(bracelet).getMaxVis(bracelet), "bracelet capacity restored");
                    tick(bracelet, 20);
                    eq(0, item(bracelet).getVis(bracelet, Aspect.AIR), "bracelet regeneration removed");
                }
            } finally {
                for (File script : parked) Files.move(new File(script.getPath() + ".parked").toPath(), script.toPath());
            }
            MineTweakerImplementationAPI.reload();
            scripted();
            System.out.println("TD_WAND_STATS_ALL_PASS checks=" + checks + " modtweaker="
                + Loader.instance().getIndexedModList().get("modtweaker2").getVersion());
        } catch (Throwable failure) {
            System.out.println("TD_WAND_STATS_FAILED checks=" + checks);
            failure.printStackTrace();
        } finally {
            server.func_71263_m();
        }
    }

    private void scripted() {
        eq(4, ConfigItems.WAND_CAP_GOLD.getCraftCost(), "script cap multiplier");
        eq(11, ConfigItems.WAND_ROD_GREATWOOD.getCraftCost(), "script core cost");
        eq(80, ConfigItems.WAND_ROD_GREATWOOD.getCapacity(), "script core capacity");
        eq(160, ConfigItems.STAFF_ROD_GREATWOOD.getCapacity(), "separate staff core");
        ItemStack wand = wand(ConfigItems.WAND_ROD_GREATWOOD, false);
        eq(8000, item(wand).getMaxVis(wand), "wand capacity");
        ItemStack sceptre = wand(ConfigItems.WAND_ROD_GREATWOOD, true);
        eq(12000, item(sceptre).getMaxVis(sceptre), "sceptre capacity bonus");
        ItemStack staff = wand(ConfigItems.STAFF_ROD_GREATWOOD, false);
        eq(16000, item(staff).getMaxVis(staff), "staff capacity");
        eq(3, item(wand).getFocusPotency(wand), "focus potency plus scripted innate bonus");
        ItemStack primal = wand(ConfigItems.STAFF_ROD_PRIMAL, false);
        eq(1, item(primal).getFocusPotency(primal), "zero innate removes native rune bonus only");
        check(item(primal).hasRunes(primal), "rune rendering unchanged");
        ItemStack empty = new ItemStack(ConfigItems.itemWandCasting);
        item(empty).setRod(empty, ConfigItems.WAND_ROD_GREATWOOD);
        eq(0, item(empty).getFocusPotency(empty), "no focus means no focus potency");

        for (Aspect aspect : Aspect.getPrimalAspects()) {
            item(wand).storeVis(wand, aspect, 12000);
        }
        tick(wand, 1);
        for (Aspect aspect : Aspect.getPrimalAspects()) eq(8000, item(wand).getVis(wand, aspect), "overcharge clamped");
        item(wand).addVis(wand, Aspect.AIR, 100, true);
        eq(8000, item(wand).getVis(wand, Aspect.AIR), "normal charging obeys capacity");
        recipes();

        ItemStack blaze = wand(ConfigItems.WAND_ROD_BLAZE, false);
        tick(blaze, 39);
        eq(0, item(blaze).getVis(blaze, Aspect.FIRE), "regen interval respected");
        tick(blaze, 40);
        eq(50, item(blaze).getVis(blaze, Aspect.FIRE), "fractional Vis amount");
        tick(blaze, 60);
        eq(100, item(blaze).getVis(blaze, Aspect.AIR), "independent aspect interval");
        eq(50, item(blaze).getVis(blaze, Aspect.FIRE), "fire not run on air interval");
        item(blaze).storeVis(blaze, Aspect.FIRE, 0);
        tick(blaze, 200);
        eq(50, item(blaze).getVis(blaze, Aspect.FIRE), "vanilla callback not doubled");
        item(blaze).storeVis(blaze, Aspect.FIRE, 1490);
        tick(blaze, 240);
        eq(1500, item(blaze).getVis(blaze, Aspect.FIRE), "ceiling clamps partial final increment");
        tick(blaze, 280);
        eq(1500, item(blaze).getVis(blaze, Aspect.FIRE), "stops at ceiling");
        item(blaze).storeVis(blaze, Aspect.FIRE, 2000);
        tick(blaze, 320);
        eq(2000, item(blaze).getVis(blaze, Aspect.FIRE), "regen ceiling doesn't discard an external charge");
        eq(0, item(blaze).getVis(blaze, Aspect.WATER), "unspecified aspects do not regenerate");
        if (Loader.isModLoaded("thaumicbases")) bracelets();
        if (Loader.isModLoaded("ThaumicConcilium")) concilium();
    }

    private void recipes() {
        TileMagicWorkbench grid = new TileMagicWorkbench();
        grid.setInventorySlotContentsSoftly(6, new ItemStack(ConfigItems.itemWandCap, 1, 1));
        grid.setInventorySlotContentsSoftly(2, new ItemStack(ConfigItems.itemWandCap, 1, 1));
        grid.setInventorySlotContentsSoftly(4, new ItemStack(ConfigItems.itemWandRod, 1, 0));
        AspectList cost = new ArcaneWandRecipe().getAspects(grid);
        for (Aspect aspect : Aspect.getPrimalAspects()) eq(44, cost.getAmount(aspect), "actual assembly recipe");
        ItemStack result = new ArcaneWandRecipe().getCraftingResult(grid);
        check(result != null, "wand output");
        eq(44, result.func_77960_j(), "recipe output uses scripted cost");
        grid.setInventorySlotContentsSoftly(6, new ItemStack(ConfigItems.itemWandCap, 1, 1));
        grid.setInventorySlotContentsSoftly(2, new ItemStack(ConfigItems.itemResource, 1, 15));
        grid.setInventorySlotContentsSoftly(1, new ItemStack(ConfigItems.itemWandCap, 1, 1));
        grid.setInventorySlotContentsSoftly(5, new ItemStack(ConfigItems.itemWandCap, 1, 1));
        cost = new ArcaneSceptreRecipe().getAspects(grid);
        for (Aspect aspect : Aspect.getPrimalAspects()) eq(66, cost.getAmount(aspect), "actual sceptre recipe");
        check(new ArcaneSceptreRecipe().getCraftingResult(grid) != null, "sceptre output");
    }

    private void bracelets() {
        for (int meta = 0; meta < 13; meta++) {
            ItemStack stack = bracelet("thaumicbases", "castingBracelet", meta);
            eq(meta == 0 ? 1800 : meta == 1 ? 2200 : 2000, item(stack).getMaxVis(stack), "bracelet capacity metadata " + meta);
            focus(stack);
            eq(meta == 0 ? 4 : meta == 1 ? 3 : 2, item(stack).getFocusPotency(stack), "bracelet potency metadata " + meta);
        }
        ItemStack blaze = bracelet("thaumicbases", "castingBracelet", 8);
        tick(blaze, 200);
        eq(50, item(blaze).getVis(blaze, Aspect.FIRE), "bracelet exact regen overrides core and native");
        eq(0, item(blaze).getVis(blaze, Aspect.AIR), "exact program replaces wildcard program");
        ItemStack primal = bracelet("thaumicbases", "castingBracelet", 12);
        tick(primal, 200);
        eq(25, item(primal).getVis(primal, Aspect.AIR), "bracelet reset inherits wildcard");
        eq(25, total(primal), "primal callback not also running");
        ItemStack ice = bracelet("thaumicbases", "castingBracelet", 9);
        tick(ice, 200);
        eq(50, item(ice).getVis(ice, Aspect.WATER), "string metadata regeneration overload");
    }

    private void concilium() {
        for (int meta = 0; meta < 8; meta++) {
            ItemStack stack = bracelet("ThaumicConcilium", "CastingBracelet", meta);
            eq(meta == 4 ? 1800 : meta == 5 ? 2100 : 2200, item(stack).getMaxVis(stack), "Concilium capacity metadata " + meta);
        }
        ItemStack infernal = bracelet("ThaumicConcilium", "CastingBracelet", 4);
        focus(infernal);
        eq(3, item(infernal).getFocusPotency(infernal), "Concilium innate Potency");
        tick(infernal, 20);
        eq(25, item(infernal).getVis(infernal, Aspect.AIR), "Concilium scripted regeneration");
        tick(infernal, 100);
        check(item(infernal).getVis(infernal, Aspect.FIRE) > 0, "Concilium addon callback preserved");
    }

    private void nativeAndOverrides() {
        ItemStack bracket = wand(ConfigItems.WAND_ROD_GREATWOOD, false);
        bracket.func_77964_b(12345);
        IItemStack input = MineTweakerMC.getIItemStack(bracket);
        WandComponentsZen.setCastingCapacity(input, 19);
        WandComponentsZen.setCastingPotency(input, 3);
        eq(1900, item(bracket).getMaxVis(bracket), "IItemStack capacity overload");
        eq(4, item(bracket).getFocusPotency(bracket), "IItemStack potency overload");
        WandComponentsZen.disableCastingVisRegeneration(input);
        tick(bracket, 20);
        eq(0, total(bracket), "IItemStack disable overload");
        WandComponentsZen.resetCastingVisRegeneration(input);

        ItemStack ice = wand(ConfigItems.WAND_ROD_ICE, false);
        tick(ice, 200);
        eq(100, item(ice).getVis(ice, Aspect.WATER), "unmodified elemental callback");
        ItemStack reed = wand(ConfigItems.WAND_ROD_REED, false);
        tick(reed, 200);
        eq(100, item(reed).getVis(reed, Aspect.AIR), "script disable then reset returns to native");
        ItemStack primal = wand(ConfigItems.STAFF_ROD_PRIMAL, false);
        tick(primal, 50);
        eq(100, total(primal), "unmodified primal random callback");

        Runnable disabled = WandComponentStatsRegistry.disableRegeneration("ice");
        item(ice).storeVis(ice, Aspect.WATER, 0);
        tick(ice, 200);
        eq(0, total(ice), "disable native regeneration");
        Runnable reset = WandComponentStatsRegistry.resetRegeneration("ice");
        tick(ice, 200);
        eq(100, total(ice), "reset native regeneration");
        reset.run();
        item(ice).storeVis(ice, Aspect.WATER, 0);
        tick(ice, 200);
        eq(0, total(ice), "reset undo restores disabled state");
        disabled.run();
        tick(ice, 200);
        eq(100, total(ice), "disable undo restores native");

        ItemStack greatwood = wand(ConfigItems.WAND_ROD_GREATWOOD, false);
        Runnable regen = WandComponentStatsRegistry.setRegeneration("greatwood", Aspect.AIR, 1, 1, 100);
        tick(greatwood, 1);
        eq(100, total(greatwood), "regen on core with no native callback");
        Object target = WandComponentStatsRegistry.castingKey(greatwood);
        Runnable exact = WandComponentStatsRegistry.disableRegeneration(target);
        tick(greatwood, 2);
        eq(100, total(greatwood), "exact disable shadows core");
        exact.run();
        tick(greatwood, 3);
        eq(200, total(greatwood), "undo inherits core");
        regen.run();

        final int[] calls = {0};
        IWandRodOnUpdate old = ConfigItems.WAND_ROD_GREATWOOD.getOnUpdate();
        ConfigItems.WAND_ROD_GREATWOOD.setOnUpdate((stack, player) -> calls[0]++);
        Runnable custom = WandComponentStatsRegistry.setRegeneration("greatwood", Aspect.AIR, 1, 1, 100);
        try {
            tick(greatwood, 1);
            eq(1, calls[0], "unrelated addon callback preserved");
            eq(300, total(greatwood), "custom regen accompanies unrelated callback");
        } finally {
            custom.run();
            ConfigItems.WAND_ROD_GREATWOOD.setOnUpdate(old);
        }
        Runnable capacity1 = WandComponentStatsRegistry.setCoreCapacity(ConfigItems.WAND_ROD_GREATWOOD, 70);
        Runnable capacity2 = WandComponentStatsRegistry.setCoreCapacity(ConfigItems.WAND_ROD_GREATWOOD, 60);
        capacity2.run();
        eq(70, ConfigItems.WAND_ROD_GREATWOOD.getCapacity(), "nested undo previous value");
        capacity1.run();
        eq(80, ConfigItems.WAND_ROD_GREATWOOD.getCapacity(), "nested undo script value");
    }

    private void validation() {
        reject(() -> WandComponentStatsRegistry.setCoreCapacity(ConfigItems.WAND_ROD_GREATWOOD, 0));
        reject(() -> WandComponentStatsRegistry.setCoreCapacity(ConfigItems.WAND_ROD_GREATWOOD, Integer.MAX_VALUE));
        reject(() -> WandComponentStatsRegistry.setCapCost(ConfigItems.WAND_CAP_GOLD, -1));
        reject(() -> WandComponentStatsRegistry.setCapCost(ConfigItems.WAND_CAP_GOLD, 21845));
        reject(() -> WandComponentStatsRegistry.setCoreCost(ConfigItems.WAND_ROD_GREATWOOD, Integer.MAX_VALUE));
        reject(() -> WandComponentStatsRegistry.setPotency("greatwood", -1));
        reject(() -> WandComponentStatsRegistry.setRegeneration("blaze", Aspect.MAGIC, 20, 1, 10));
        reject(() -> WandComponentStatsRegistry.setRegeneration("blaze", Aspect.FIRE, 0, 1, 10));
        reject(() -> WandComponentStatsRegistry.setRegeneration("blaze", Aspect.FIRE, 20, Double.NaN, 10));
        reject(() -> WandComponentStatsRegistry.setRegeneration("blaze", Aspect.FIRE, 20, Double.POSITIVE_INFINITY, 10));
        reject(() -> WandComponentStatsRegistry.setRegeneration("blaze", Aspect.FIRE, 20, 0, 10));
        reject(() -> WandComponentStatsRegistry.setRegeneration("blaze", Aspect.FIRE, 20, 1, 101));
        eq(4, ConfigItems.WAND_CAP_GOLD.getCraftCost(), "invalid cost doesn't partially change cap");
    }

    private void toggle() {
        configure(false);
        try {
            eq(3, ConfigItems.WAND_CAP_GOLD.getCraftCost(), "disabled native cap value");
            eq(50, ConfigItems.WAND_ROD_GREATWOOD.getCapacity(), "disabled native capacity");
            ItemStack wand = wand(ConfigItems.WAND_ROD_GREATWOOD, false);
            eq(1, item(wand).getFocusPotency(wand), "disabled potency");
            ItemStack blaze = wand(ConfigItems.WAND_ROD_BLAZE, false);
            tick(blaze, 200);
            eq(100, item(blaze).getVis(blaze, Aspect.FIRE), "disabled native regen");
            eq(0, item(blaze).getVis(blaze, Aspect.AIR), "disabled custom regen");
            if (Loader.isModLoaded("thaumicbases")) {
                ItemStack bracelet = bracelet("thaumicbases", "castingBracelet", 0);
                eq(1000, item(bracelet).getMaxVis(bracelet), "disabled bracelet capacity");
            }
        } finally {
            configure(true);
        }
        eq(4, ConfigItems.WAND_CAP_GOLD.getCraftCost(), "reenabled cap value");
        eq(80, ConfigItems.WAND_ROD_GREATWOOD.getCapacity(), "reenabled capacity");
    }

    private void configure(boolean enabled) {
        Config.getRawConfig().get("features.wandComponentStats", "enabled", true).set(enabled);
        Config.getRawConfig().save();
        ThaumicDabblery.proxy.onConfigReload();
    }

    private ItemStack wand(WandRod rod, boolean sceptre) {
        ItemStack stack = new ItemStack(ConfigItems.itemWandCasting);
        item(stack).setRod(stack, rod);
        item(stack).setCap(stack, ConfigItems.WAND_CAP_GOLD);
        if (sceptre) stack.field_77990_d.func_74757_a("sceptre", true);
        focus(stack);
        return stack;
    }

    private void focus(ItemStack stack) {
        ItemStack focus = new ItemStack(ConfigItems.itemFocusFire);
        ((ItemFocusBasic) focus.func_77973_b()).applyUpgrade(focus, FocusUpgradeType.potency, 1);
        item(stack).setFocus(stack, focus);
    }

    private ItemStack bracelet(String modid, String name, int metadata) {
        Item item = GameRegistry.findItem(modid, name);
        check(item instanceof ItemWandCasting, "bracelet registered: " + modid);
        return new ItemStack(item, 1, metadata);
    }

    private ItemWandCasting item(ItemStack stack) {
        return (ItemWandCasting) stack.func_77973_b();
    }

    private void tick(ItemStack stack, int ticks) {
        player.field_70173_aa = ticks;
        item(stack).func_77663_a(stack, player.field_70170_p, player, 0, false);
    }

    private int total(ItemStack stack) {
        int sum = 0;
        for (Aspect aspect : Aspect.getPrimalAspects()) sum += item(stack).getVis(stack, aspect);
        return sum;
    }

    private void reject(Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("Invalid value was accepted");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    private void eq(int expected, int actual, String description) {
        check(expected == actual, description + ": expected " + expected + ", got " + actual);
    }

    private void check(boolean result, String description) {
        if (!result) throw new AssertionError(description);
        checks++;
    }
}
