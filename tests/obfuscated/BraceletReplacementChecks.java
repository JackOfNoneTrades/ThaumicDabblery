package tdtest;

import java.util.*;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.launchwrapper.Launch;
import org.fentanylsolutions.thaumicdabblery.compat.salisarcana.BraceletReplacementGuard;
import dev.rndmorris.salisarcana.common.recipes.ReplaceWandCapsRecipe;
import dev.rndmorris.salisarcana.common.recipes.ReplaceWandCoreRecipe;
import dev.rndmorris.salisarcana.lib.WandHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.research.ResearchManager;

public final class BraceletReplacementChecks {
    public int checks;
    public void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    public List<ItemStack> bracelets() throws Exception {
        List<ItemStack> result = new ArrayList<>();
        for (String[] id : new String[][] {{"thaumicbases", "castingBracelet"}, {"ThaumicConcilium", "CastingBracelet"}}) {
            if (!Loader.isModLoaded(id[0])) continue;
            Item item = GameRegistry.findItem(id[0], id[1]);
            check(item != null, "bracelet registered " + id[0]);
            String[] names = (String[]) item.getClass().getField("names").get(null);
            for (int meta = 0; meta < names.length; meta++) {
                ItemStack stack = new ItemStack(item, 1, meta);
                stack.func_77982_d(new NBTTagCompound());
                stack.func_77978_p().func_74778_a("td_marker", id[0] + ":" + meta);
                result.add(stack);
            }
        }
        return result;
    }
    public ItemStack wand(boolean staff, boolean sceptre) {
        ItemStack stack = WandHelper.createWand(staff ? ConfigItems.STAFF_ROD_GREATWOOD : ConfigItems.WAND_ROD_GREATWOOD, ConfigItems.WAND_CAP_IRON, sceptre);
        stack.func_77978_p().func_74778_a("td_marker", "ordinary wand");
        return stack;
    }
    public InventoryBasic capGrid(ItemStack input, int at, boolean sceptre) {
        InventoryBasic inv = new InventoryBasic("replacement probe", true, 11);
        inv.func_70299_a(at, input);
        inv.func_70299_a((at + 1) % 9, ConfigItems.WAND_CAP_GOLD.getItem().func_77946_l());
        inv.func_70299_a((at + 2) % 9, ConfigItems.WAND_CAP_GOLD.getItem().func_77946_l());
        if (sceptre) inv.func_70299_a((at + 3) % 9, ConfigItems.WAND_CAP_GOLD.getItem().func_77946_l());
        return inv;
    }
    public InventoryBasic coreGrid(ItemStack input, int at, boolean staff) {
        InventoryBasic inv = new InventoryBasic("replacement probe", true, 11);
        inv.func_70299_a(at, input);
        inv.func_70299_a((at + 1) % 9, (staff ? ConfigItems.STAFF_ROD_SILVERWOOD : ConfigItems.WAND_ROD_SILVERWOOD).getItem().func_77946_l());
        return inv;
    }
    public void run(EntityPlayer player) throws Exception {
        check(Boolean.FALSE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment")), "obfuscated runtime");
        ReplaceWandCapsRecipe caps = new ReplaceWandCapsRecipe();
        ReplaceWandCoreRecipe cores = new ReplaceWandCoreRecipe();
        ResearchManager.completeResearchUnsaved(player.func_70005_c_(), caps.getResearch());
        ResearchManager.completeResearchUnsaved(player.func_70005_c_(), cores.getResearch());
        ResearchManager.completeResearchUnsaved(player.func_70005_c_(), "SCEPTRE");
        for (WandCap cap : WandCap.caps.values()) ResearchManager.completeResearchUnsaved(player.func_70005_c_(), cap.getResearch());
        for (WandRod rod : WandRod.rods.values()) ResearchManager.completeResearchUnsaved(player.func_70005_c_(), rod.getResearch());
        List<ItemStack> bracelets = bracelets();
        for (ItemStack bracelet : bracelets) {
            check(BraceletReplacementGuard.isBracelet(bracelet), "all bracelet variants recognized");
            ItemStack before = bracelet.func_77946_l();
            for (int slot = 0; slot < 9; slot++) {
                InventoryBasic capInv = capGrid(bracelet, slot, false), coreInv = coreGrid(bracelet, slot, true);
                check(!caps.matches(capInv, player.field_70170_p, player), "bracelet cap recipe rejected");
                check(caps.getCraftingResult(capInv) == null && caps.getAspects(capInv) == null, "no cap output or cost");
                check(!cores.matches(coreInv, player.field_70170_p, player), "bracelet core recipe rejected");
                check(cores.getCraftingResult(coreInv) == null && cores.getAspects(coreInv) == null, "no core output or cost");
                check(caps.salisArcana$getResearches(capInv, player.field_70170_p, player).length == 1, "cap research query tolerates rejected bracelet");
                check(cores.salisArcana$getResearches(coreInv, player.field_70170_p, player).length == 1, "core research query tolerates rejected bracelet");
            }
            check(ItemStack.func_77989_b(before, bracelet), "rejected bracelet metadata and NBT unchanged");
            check(WandHelper.getWandItem(bracelet) == bracelet.func_77973_b(), "bracelet still recognized as casting item");
        }
        for (boolean staff : new boolean[]{false, true}) for (boolean sceptre : new boolean[]{false, true}) {
            ItemStack input = wand(staff, sceptre), before = input.func_77946_l();
            check(!BraceletReplacementGuard.isBracelet(input), "ordinary wand not blocked");
            InventoryBasic capInv = capGrid(input, 0, sceptre), coreInv = coreGrid(input, 0, staff);
            // A bracelet in slot 10 powers the table, rather than being a replacement ingredient.
            if (!bracelets.isEmpty()) {
                capInv.func_70299_a(10, bracelets.get(0));
                coreInv.func_70299_a(10, bracelets.get(0));
            }
            check(caps.matches(capInv, player.field_70170_p, player), "ordinary cap recipe with bracelet power slot");
            check(cores.matches(coreInv, player.field_70170_p, player), "ordinary core recipe with bracelet power slot");
            ItemStack capOut = caps.getCraftingResult(capInv), coreOut = cores.getCraftingResult(coreInv);
            check(capOut != null && ((ItemWandCasting)capOut.func_77973_b()).getCap(capOut) == ConfigItems.WAND_CAP_GOLD, "ordinary cap replaced");
            check(coreOut != null && ((ItemWandCasting)coreOut.func_77973_b()).getRod(coreOut) == (staff ? ConfigItems.STAFF_ROD_SILVERWOOD : ConfigItems.WAND_ROD_SILVERWOOD), "ordinary core replaced");
            check(capOut.func_77978_p().func_74779_i("td_marker").equals("ordinary wand") && coreOut.func_77978_p().func_74779_i("td_marker").equals("ordinary wand"), "ordinary outputs retain custom NBT");
            check(ItemStack.func_77989_b(before, input), "ordinary recipe does not mutate input");
            check(caps.getAspects(capInv).size() == 6 && cores.getAspects(coreInv).size() == 6, "ordinary recipes retain Vis costs");
        }
        for (ItemStack bracelet : bracelets) if (bracelet.func_77960_j() == 0) {
            ItemWandCasting item = (ItemWandCasting) bracelet.func_77973_b();
            check(item.getMaxVis(bracelet) == 1800, "scripted bracelet capacity retained");
            item.storeVis(bracelet, Aspect.AIR, 100);
            check(item.getVis(bracelet, Aspect.AIR) == 100, "bracelet charging retained");
            check(item.consumeVis(bracelet, player, Aspect.AIR, 10, true), "bracelet Vis consumption retained");
        }
    }
}
