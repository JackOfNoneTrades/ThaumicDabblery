package tdtest;

import java.util.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.oredict.OreDictionary;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import baubles.api.*;
import baubles.api.expanded.*;
import baubles.common.container.*;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.*;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.container.ContainerFocusPouch;
import thaumcraft.common.items.wands.ItemFocusPouch;
import witchinggadgets.common.WGContent;
import witchinggadgets.common.CommonProxy;
import witchinggadgets.common.gui.ContainerCloak;
import witchinggadgets.common.items.baubles.ItemCloak;
import witchinggadgets.common.util.Utilities;
import witchinggadgets.common.util.Lib;
import witchinggadgets.common.util.handler.EventHandler;
import witchinggadgets.common.util.handler.PlayerTickHandler;

import static tdtest.BaubleSlotChecks.slot;
public final class WitchingBaubleSlotChecks {
    public static ItemStack[] stored(ItemStack stack) throws Exception {
        // Read the persisted inventory: the accessor changed staticness, and legacy WG leaves client-only
        // methods on ItemCloak, preventing getMethod() enumeration on a dedicated server.
        ItemStack[] stored = new ItemStack[27];
        if (!stack.func_77942_o()) return stored;
        net.minecraft.nbt.NBTTagList list = stack.func_77978_p().func_150295_c("InternalInventory", 10);
        for (int i = 0; i < list.func_74745_c(); i++) {
            NBTTagCompound entry = list.func_150305_b(i);
            int slot = entry.func_74771_c("Slot") & 255;
            if (slot < stored.length) stored[slot] = ItemStack.func_77949_a(entry);
        }
        return stored;
    }
    public static void run(BaubleSlotChecks c, EntityPlayer player) throws Exception {
        WitchingBaubleScriptChecks.run(c);
        c.empty(player);
        InventoryBaubles inv = (InventoryBaubles) BaublesApi.getBaubles(player);
        int ring = slot("ring"), amulet = slot("amulet"), belt = slot("belt"), charm = slot("charm");
        ItemStack haste = new ItemStack(WGContent.ItemMagicalBaubles, 1, 3);
        inv.func_70299_a(amulet, haste);
        player.field_70122_E = false;
        player.field_70170_p.func_147465_d(0, 5, 0, Blocks.field_150348_b, 0, 3);
        PlayerEvent.BreakSpeed breaking = new PlayerEvent.BreakSpeed(player, Blocks.field_150348_b, 0, 1, 0, 5, 0);
        new EventHandler().onPlayerBreaking(breaking);
        c.check(breaking.newSpeed == 5.0F, "moved haste vambrace still works");
        c.empty(player);
        ItemStack sniper = new ItemStack(WGContent.ItemMagicalBaubles, 1, WitchingBaubleScriptChecks.sniperMetadata());
        inv.func_70299_a(belt, sniper);
        ItemStack bow = new ItemStack(Items.field_151031_f);
        player.field_71071_by.field_70462_a[0] = bow; player.func_71008_a(bow, 1000);
        PlayerTickHandler handler = new PlayerTickHandler();
        handler.playerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player));
        c.check(player.func_110148_a(SharedMonsterAttributes.field_111263_d).func_111127_a(new UUID(Lib.ATTRIBUTE_MOD_UUID, 6)) != null, "actual sniper ring works from belt");
        handler.playerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        player.func_71034_by();
        c.empty(player);
        ItemStack cloak = new ItemStack(WGContent.ItemCloak, 1, 2);
        ItemStack otherCloak = new ItemStack(WGContent.ItemCloak, 1, 2);
        inv.func_70299_a(ring, cloak);
        inv.func_70299_a(slot("cape"), otherCloak);
        c.check(Arrays.asList(Utilities.getActiveMagicalCloak(player)).contains(cloak), "moved cloak discovered");
        ContainerCloak bag = (ContainerCloak) new CommonProxy().getServerGuiElement(4, player, player.field_70170_p, 0, 5, 0);
        ((net.minecraft.inventory.IInventory) bag.getClass().getField("input").get(bag)).func_70299_a(0, new ItemStack(Items.field_151045_i));
        bag.func_75134_a(player);
        c.check(stored(cloak)[0].func_77973_b() == Items.field_151045_i, "moved storage cloak saves");
        c.check(stored(otherCloak)[0] == null, "same-metadata cloak not overwritten");
        c.empty(player);
        ItemStack kama = new ItemStack(WGContent.ItemKama, 1, 2); inv.func_70299_a(amulet, kama);
        c.check(WitchingBaubleSlots.storage(player, 5) == kama, "moved storage kama selected");
        c.check(Arrays.asList(Utilities.getActiveMagicalCloak(player)).contains(kama), "kama included in cloak effects");
        c.empty(player);
        ItemStack pouch = new ItemStack(ConfigItems.itemFocusPouch);
        ItemStack otherPouch = pouch.func_77946_l();
        ItemStack heldPouch = pouch.func_77946_l();
        inv.func_70299_a(charm, pouch); inv.func_70299_a(belt, otherPouch);
        player.field_71071_by.field_70462_a[0] = heldPouch;
        c.check(charm >= 4, "pouch test uses expanded slot");
        String pouchClass = cpw.mods.fml.common.Loader.isModLoaded("TravellersGear")
            ? "witchinggadgets.asm.pouch.ContainerPatchedFocusPouch" : "witchinggadgets.common.pouch.ContainerPatchedFocusPouch";
        ContainerFocusPouch focusBag = (ContainerFocusPouch) Class.forName(pouchClass)
            .getConstructor(net.minecraft.entity.player.InventoryPlayer.class, net.minecraft.world.World.class, int.class, int.class, int.class)
            .newInstance(player.field_71071_by, player.field_70170_p, 0, 5, 0);
        focusBag.input.func_70299_a(0, new ItemStack(ConfigItems.itemFocusFire));
        focusBag.func_75134_a(player);
        ItemFocusPouch item = (ItemFocusPouch) pouch.func_77973_b();
        c.check(item.getInventory(pouch)[0] != null, "expanded-slot focus pouch saves");
        c.check(item.getInventory(otherPouch)[0] == null && item.getInventory(heldPouch)[0] == null, "belt and held pouches not overwritten");
        c.check(player.field_71071_by.field_70462_a[0] == heldPouch && inv.func_70301_a(charm) == pouch, "no pouch replacement/duplication");
        c.empty(player);
        if (cpw.mods.fml.common.Loader.isModLoaded("TravellersGear")) WitchingBaubleLegacyServerChecks.run(c, player);
    }
}
