package tdtest;

import java.util.LinkedHashMap;
import java.util.Map;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.fentanylsolutions.thaumicdabblery.Config;
import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;
import org.fentanylsolutions.thaumicdabblery.feature.wandcomponents.WandComponentStatsRegistry;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.wands.StaffRod;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.crafting.ArcaneSceptreRecipe;
import thaumcraft.common.lib.crafting.ArcaneWandRecipe;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.tiles.TileMagicWorkbench;

/** Shared production recipe checks, called by the server and client probes. No game is launched here. */
public final class WandCraftingCostChecks {
    private int checks;
    private EntityPlayer player;

    public int run(EntityPlayer player) {
        this.player = player;
        Map<WandCap, Integer> caps = new LinkedHashMap<>();
        Map<WandRod, Integer> rods = new LinkedHashMap<>();
        for (WandCap cap : WandCap.caps.values()) caps.put(cap, cap.getCraftCost());
        for (WandRod rod : WandRod.rods.values()) rods.put(rod, rod.getCraftCost());

        if (Loader.isModLoaded("ForbiddenMagic")) {
            eq(1000, WandCap.caps.get("orichalcum").getCraftCost(), "creative cap remains unchanged");
            eq(1000, WandRod.rods.get("neutronium").getCraftCost(), "creative rod remains unchanged");
            eq(1000, WandRod.rods.get("neutronium_staff").getCraftCost(), "creative staff remains unchanged");
        }

        WandRod greatwood = ConfigItems.WAND_ROD_GREATWOOD;
        WandCap gold = ConfigItems.WAND_CAP_GOLD;
        for (String id : new String[] {"greatwood", "silverwood", "greatwood_staff", "silverwood_staff", "primal_staff"}) {
            WandRod rod = WandRod.rods.get(id);
            for (int cost : new int[] {22, 24, 32}) {
                withCosts(gold, rod, 4, cost, cost * 4, cost * 6);
            }
        }
        for (int multiplier : new int[] {22, 24, 32}) {
            withCosts(gold, greatwood, multiplier, 8, multiplier * 8, multiplier * 12);
        }

        // The same components may be safe for a wand/staff and unsafe for a sceptre.
        withCosts(gold, greatwood, 1, 21845, 21845, 32767);
        withCosts(gold, greatwood, 1, 21846, 21846, -1);
        withCosts(gold, greatwood, 1, 32767, 32767, -1);
        withCosts(gold, ConfigItems.STAFF_ROD_GREATWOOD, 1, 32767, 32767, -1);
        withCosts(gold, greatwood, 32767, 1, 32767, -1);
        withCosts(gold, greatwood, 2, 16383, 32766, -1);
        withCosts(gold, greatwood, 2, 16384, -1, -1);
        withCosts(gold, greatwood, 32767, 32767, -1, -1);
        withCosts(gold, greatwood, 0, 32767, 0, 0);
        withCosts(gold, greatwood, 32767, 0, 0, 0);

        // Other mods can supply arbitrary ints without going through our setters.
        int oldCap = gold.getCraftCost();
        int oldCore = greatwood.getCraftCost();
        try {
            for (int[] pair : new int[][] {{65536, 65536}, {Integer.MAX_VALUE, Integer.MAX_VALUE}, {-1, -1}, {-1, 0}}) {
                gold.setCraftCost(pair[0]);
                greatwood.setCraftCost(pair[1]);
                recipe(gold, greatwood, false, -1);
                recipe(gold, greatwood, true, -1);
            }
        } finally {
            gold.setCraftCost(oldCap);
            greatwood.setCraftCost(oldCore);
        }

        // Applying while disabled and later enabling must not depend on counterpart registration/order.
        configure(false);
        Runnable capUndo = null;
        Runnable coreUndo = null;
        try {
            capUndo = WandComponentStatsRegistry.setCapCost(gold, 32767);
            coreUndo = WandComponentStatsRegistry.setCoreCost(greatwood, 1);
            eq(3, gold.getCraftCost(), "disabled cap remains native");
            eq(3, greatwood.getCraftCost(), "disabled core remains native");
            configure(true);
            recipe(gold, greatwood, false, 32767);
            recipe(gold, greatwood, true, -1);
            configure(false);
            recipe(gold, greatwood, false, 9);
            recipe(gold, greatwood, true, 13);
        } finally {
            if (coreUndo != null) coreUndo.run();
            if (capUndo != null) capUndo.run();
            configure(true);
        }

        // Empty/unrecognized ingredients stay non-recipes, with no crashes or accidental outputs.
        for (IArcaneRecipe recipe : new IArcaneRecipe[] {new ArcaneWandRecipe(), new ArcaneSceptreRecipe()}) {
            TileMagicWorkbench empty = new TileMagicWorkbench();
            check(recipe.getCraftingResult(empty) == null, "empty grid has no output");
            eq(0, recipe.getAspects(empty).size(), "empty grid has no cost");
        }
        for (Map.Entry<WandCap, Integer> entry : caps.entrySet()) {
            eq(entry.getValue(), entry.getKey().getCraftCost(), "all cap values restored: " + entry.getKey().getTag());
        }
        for (Map.Entry<WandRod, Integer> entry : rods.entrySet()) {
            eq(entry.getValue(), entry.getKey().getCraftCost(), "all core values restored: " + entry.getKey().getTag());
        }
        System.out.println("TD_WAND_CRAFTING_PASS checks=" + checks + " forbiddenmagic=" + Loader.isModLoaded("ForbiddenMagic"));
        return checks;
    }

    private void withCosts(WandCap cap, WandRod rod, int capCost, int coreCost, int wandCost, int sceptreCost) {
        Runnable undoCap = WandComponentStatsRegistry.setCapCost(cap, capCost);
        try {
            Runnable undoCore = WandComponentStatsRegistry.setCoreCost(rod, coreCost);
            try {
                eq(capCost, cap.getCraftCost(), "cap edit accepted independently");
                eq(coreCost, rod.getCraftCost(), "core edit accepted independently");
                recipe(cap, rod, false, wandCost);
                if (!(rod instanceof StaffRod)) recipe(cap, rod, true, sceptreCost);
            } finally {
                undoCore.run();
            }
        } finally {
            undoCap.run();
        }
    }

    private void recipe(WandCap cap, WandRod rod, boolean sceptre, int expected) {
        IArcaneRecipe recipe = sceptre ? new ArcaneSceptreRecipe() : new ArcaneWandRecipe();
        TileMagicWorkbench grid = new TileMagicWorkbench();
        grid.setInventorySlotContentsSoftly(4, rod.getItem().func_77946_l());
        grid.setInventorySlotContentsSoftly(6, cap.getItem().func_77946_l());
        grid.setInventorySlotContentsSoftly(2, sceptre ? new ItemStack(ConfigItems.itemResource, 1, 15) : cap.getItem().func_77946_l());
        if (sceptre) {
            grid.setInventorySlotContentsSoftly(1, cap.getItem().func_77946_l());
            grid.setInventorySlotContentsSoftly(5, cap.getItem().func_77946_l());
        }
        String description = (sceptre ? "sceptre " : "wand/staff ") + cap.getTag() + "/" + rod.getTag()
            + " " + cap.getCraftCost() + "*" + rod.getCraftCost();
        if (player != null) {
            ResearchManager.completeResearchUnsaved(player.func_70005_c_(), cap.getResearch());
            ResearchManager.completeResearchUnsaved(player.func_70005_c_(), rod.getResearch());
            ResearchManager.completeResearchUnsaved(player.func_70005_c_(), "SCEPTRE");
            check(recipe.matches(grid, player.field_70170_p, player) == (expected >= 0), description + " matches");
        }
        ItemStack output = recipe.getCraftingResult(grid);
        AspectList aspects = recipe.getAspects(grid);
        if (expected < 0) {
            check(output == null, description + " unsafe output blocked");
            eq(0, aspects.size(), description + " unsafe cost not exposed");
        } else {
            check(output != null, description + " output present");
            eq(expected, output.func_77960_j(), description + " metadata");
            for (Aspect aspect : Aspect.getPrimalAspects()) eq(expected, aspects.getAmount(aspect), description + " Vis");
            NBTTagCompound saved = new NBTTagCompound();
            output.func_77955_b(saved);
            ItemStack restored = ItemStack.func_77949_a(saved);
            eq(expected, restored.func_77960_j(), description + " metadata survives saving");
            ItemWandCasting wand = (ItemWandCasting) restored.func_77973_b();
            check(wand.getRod(restored) == rod && wand.getCap(restored) == cap, description + " components survive saving");
        }
    }

    private void configure(boolean enabled) {
        Config.getRawConfig().get("features.wandComponentStats", "enabled", true).set(enabled);
        Config.getRawConfig().save();
        ThaumicDabblery.proxy.onConfigReload();
    }

    private void eq(int expected, int actual, String message) {
        check(expected == actual, message + ": expected " + expected + ", got " + actual);
    }

    private void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
        checks++;
    }
}
