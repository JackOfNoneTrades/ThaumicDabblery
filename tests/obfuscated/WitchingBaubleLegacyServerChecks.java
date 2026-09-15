package tdtest;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import baubles.api.*;
import baubles.common.container.InventoryBaubles;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.*;
import travellersgear.api.TravellersGearAPI;
import travellersgear.common.network.MessageActiveAbility;
import travellersgear.common.util.TGEventHandler;
import thaumcraft.api.aspects.*;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.baubles.ItemAmuletVis;
import witchinggadgets.common.WGContent;
import witchinggadgets.common.CommonProxy;
import witchinggadgets.common.gui.ContainerCloak;
import witchinggadgets.common.items.baubles.ItemCloak;
import witchinggadgets.common.util.Utilities;
import witchinggadgets.common.util.handler.EventHandler;
import static tdtest.WitchingBaubleSlotChecks.stored;

public final class WitchingBaubleLegacyServerChecks {
    public static final class GuiPlayer extends net.minecraftforge.common.util.FakePlayer {
        int gui = -1, x, y;
        GuiPlayer(net.minecraft.world.WorldServer world) {
            super(world, new com.mojang.authlib.GameProfile(java.util.UUID.fromString("6c93aed7-b083-47a4-95b2-d0cb6cd1aee2"), "tdstorage"));
        }
        @Override public void openGui(Object mod, int id, net.minecraft.world.World world, int x, int y, int z) {
            this.gui = id; this.x = x; this.y = y;
        }
    }

    public static void run(BaubleSlotChecks c, EntityPlayer player) throws Exception {
        InventoryBaubles inv = (InventoryBaubles) BaublesApi.getBaubles(player);
        ItemStack[] saved = TravellersGearAPI.getExtendedInventory(player);
        int expanded = BaubleSlotChecks.slot("charm"), ring = BaubleSlotChecks.slot("ring");
        c.check(expanded >= 4, "legacy suite includes expanded slots");
        try {
            ItemStack nativeStorage = new ItemStack(WGContent.ItemCloak, 1, 2);
            ItemStack nativeHaste = new ItemStack(WGContent.ItemMagicalBaubles, 1, 3);
            TravellersGearAPI.setExtendedInventory(player, new ItemStack[]{nativeStorage, null, nativeHaste, null});
            PlayerEvent.BreakSpeed breaking = new PlayerEvent.BreakSpeed(player, Blocks.field_150348_b, 0, 1, 0, 5, 0);
            player.field_70122_E = false;
            new EventHandler().onPlayerBreaking(breaking);
            c.check(breaking.newSpeed == 5f, "native Traveller's Gear haste remains functional");
            c.check(Utilities.getActiveMagicalCloak(player).length == 1, "native TG cloak remains discoverable");

            ItemStack moved = new ItemStack(WGContent.ItemCloak, 1, 2);
            inv.func_70299_a(ring, moved);
            c.check(Utilities.getActiveMagicalCloak(player).length == 2, "native and moved cloaks both discovered");
            ContainerCloak movedBag = (ContainerCloak) new CommonProxy().getServerGuiElement(4, player, player.field_70170_p, ring, LegacyWitchingBaubles.GUI_MARKER, 0);
            net.minecraft.inventory.IInventory movedInput = (net.minecraft.inventory.IInventory) movedBag.getClass().getField("input").get(movedBag);
            movedInput.func_70299_a(0, new ItemStack(Items.field_151045_i)); movedBag.func_75134_a(player);
            c.check(stored(moved)[0] != null, "explicit moved storage saves");
            nativeStorage = TravellersGearAPI.getExtendedInventory(player)[0];
            c.check(stored(nativeStorage)[0] == null, "native same-meta cloak not overwritten");
            ContainerCloak nativeBag = (ContainerCloak) new CommonProxy().getServerGuiElement(4, player, player.field_70170_p, 0, 5, 0);
            ((net.minecraft.inventory.IInventory) nativeBag.getClass().getField("input").get(nativeBag)).func_70299_a(1, new ItemStack(Items.field_151043_k)); nativeBag.func_75134_a(player);
            nativeStorage = TravellersGearAPI.getExtendedInventory(player)[0];
            c.check(stored(nativeStorage)[1] != null, "native TG storage still saves");
            c.check(stored(moved)[1] == null, "native save does not overwrite moved cloak");
            inv.func_70299_a(ring, null);
            movedInput.func_70299_a(2, new ItemStack(Items.field_151043_k)); movedBag.func_75134_a(player);
            nativeStorage = TravellersGearAPI.getExtendedInventory(player)[0];
            c.check(stored(nativeStorage)[2] == null,
                "closing moved bag after unequip does not overwrite native TG cloak");
            c.check(new CommonProxy().getServerGuiElement(4, player, player.field_70170_p, 999, LegacyWitchingBaubles.GUI_MARKER, 0) == null, "invalid storage slot rejected");
            c.empty(player);

            GuiPlayer recording = new GuiPlayer((net.minecraft.world.WorldServer) player.field_70170_p);
            InventoryBaubles recordingInv = (InventoryBaubles) BaublesApi.getBaubles(recording);
            ItemStack selectedStorage = new ItemStack(WGContent.ItemCloak, 1, 2);
            recordingInv.func_70299_a(ring, selectedStorage);
            ((travellersgear.api.IActiveAbility) selectedStorage.func_77973_b()).activate(recording, selectedStorage);
            c.check(recording.gui == 4 && recording.x == ring && recording.y == LegacyWitchingBaubles.GUI_MARKER,
                "actual cloak activation carries exact bauble slot into GUI packet");
            c.empty(recording);

            ItemStack wolf = new ItemStack(WGContent.ItemCloak, 1, 3);
            BaubleRules.Change wolfRule = BaubleRules.edit(wolf, 0, new String[]{"charm"});
            try {
                inv.func_70299_a(expanded, wolf);
                c.check(wolf.func_77973_b() instanceof IBauble, "legacy cloak has bauble callback bridge");
                Object[][] entries = new TGEventHandler().buildEventGearList(player);
                int encoded = -1;
                for (Object[] entry : entries) if (entry[0] == wolf) encoded = (Integer) entry[1];
                c.check(encoded == LegacyWitchingBaubles.SLOT_BASE + expanded, "events encode expanded slot without TG collision");
                TGEventHandler.triggerGear(player, encoded, new LivingHurtEvent(player, DamageSource.field_76377_j, 9));
                c.check(wolf.func_77978_p().func_74762_e("wolfPotion") == 2, "wolf event targets moved cloak");
                c.check(!TravellersGearAPI.getExtendedInventory(player)[0].func_77942_o()
                    || !TravellersGearAPI.getExtendedInventory(player)[0].func_77978_p().func_74764_b("wolfPotion"), "event leaves native TG cloak untouched");
                player.field_70173_aa = Math.max(2, player.field_70173_aa);
                ((IBauble) BaubleHooks.dispatch(wolf)).onWornTick(wolf, player);
                c.check(player.func_70644_a(Potion.field_76420_g), "bauble tick applies wolf strength");
                c.check(!wolf.func_77942_o() || !wolf.func_77978_p().func_74764_b("wolfPotion"), "wolf event consumed once");
            } finally { c.empty(player); wolfRule.undo(); }

            ItemStack raven = new ItemStack(WGContent.ItemCloak, 1, 4);
            BaubleRules.Change ravenRule = BaubleRules.edit(raven, 0, new String[]{"charm"});
            try {
                inv.func_70299_a(expanded, raven);
                MessageActiveAbility.performAbility(player, LegacyWitchingBaubles.SLOT_BASE + expanded);
                c.check(raven.func_77978_p().func_74767_n("noGlide"), "legacy radial activation reaches moved raven cloak");
                MessageActiveAbility.performAbility(player, LegacyWitchingBaubles.SLOT_BASE + expanded);
                c.check(!raven.func_77978_p().func_74767_n("noGlide"), "second activation toggles back");
                player.field_70122_E = false; player.field_71075_bZ.field_75100_b = false;
                player.field_70181_x = -0.5; player.field_70143_R = 5;
                ((IBauble) BaubleHooks.dispatch(raven)).onWornTick(raven, player);
                c.check(player.field_70181_x > -0.5 && player.field_70143_R == 0, "moved raven cloak actually glides");
                raven.func_77978_p().func_74757_a("isSpectral", true);
                ((IBauble) BaubleHooks.dispatch(raven)).onUnequipped(raven, player);
                c.check(!raven.func_77978_p().func_74767_n("isSpectral"), "legacy unequip callback runs");
                ItemStack before = raven.func_77946_l();
                MessageActiveAbility.performAbility(player, Integer.MAX_VALUE);
                MessageActiveAbility.performAbility(player, LegacyWitchingBaubles.SLOT_BASE - 1);
                c.check(ItemStack.func_77989_b(before, raven), "invalid activation IDs do nothing");
                java.lang.reflect.Field enabled = BaubleSlotsFeature.class.getDeclaredField("enabled");
                enabled.setAccessible(true);
                boolean wasEnabled = enabled.getBoolean(null);
                try {
                    enabled.setBoolean(null, false);
                    MessageActiveAbility.performAbility(player, LegacyWitchingBaubles.SLOT_BASE + expanded);
                    c.check(ItemStack.func_77989_b(before, raven), "feature disable blocks extended activation");
                } finally { enabled.setBoolean(null, wasEnabled); }
            } finally { c.empty(player); ravenRule.undo(); }

            ItemStack amulet = new ItemStack(ConfigItems.itemAmuletVis, 1, 1);
            BaubleRules.Change amuletRule = BaubleRules.edit(amulet, 0, new String[]{"charm"});
            try {
                inv.func_70299_a(expanded, amulet);
                ItemAmuletVis item = (ItemAmuletVis) amulet.func_77973_b();
                item.storeVis(amulet, Aspect.AIR, 500);
                c.check(Utilities.consumeVisFromInventoryWithoutDiscount(player, new AspectList().add(Aspect.AIR, 1)), "Vis found in expanded-slot amulet");
                c.check(item.getVis(amulet, Aspect.AIR) < 500, "expanded amulet actually pays Vis");
            } finally { c.empty(player); amuletRule.undo(); }
        } finally {
            c.empty(player); TravellersGearAPI.setExtendedInventory(player, saved);
        }
    }
}
