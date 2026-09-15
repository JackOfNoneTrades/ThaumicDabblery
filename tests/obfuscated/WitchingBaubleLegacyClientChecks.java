package tdtest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import baubles.api.BaublesApi;
import baubles.common.container.InventoryBaubles;
import cpw.mods.fml.common.gameevent.TickEvent;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.*;
import travellersgear.api.TravellersGearAPI;
import travellersgear.client.handlers.ActiveAbilityHandler;
import travellersgear.common.network.MessageActiveAbility;
import witchinggadgets.common.WGContent;
import witchinggadgets.common.util.WGKeyHandler;
import io.netty.buffer.Unpooled;
import io.netty.buffer.ByteBuf;

public final class WitchingBaubleLegacyClientChecks {
    public static void run(BaubleSlotChecks c, Minecraft mc) throws Exception {
        EntityPlayer player = mc.field_71439_g;
        InventoryBaubles inv = (InventoryBaubles) BaublesApi.getBaubles(player);
        ItemStack[] saved = TravellersGearAPI.getExtendedInventory(player);
        int expanded = BaubleSlotChecks.slot("charm");
        c.check(expanded >= 4, "legacy client has extra slots");
        c.empty(player);
        ItemStack raven = new ItemStack(WGContent.ItemCloak, 1, 4);
        BaubleRules.Change rule = BaubleRules.edit(raven, 0, new String[]{"charm"});
        try {
            TravellersGearAPI.setExtendedInventory(player, new ItemStack[]{new ItemStack(WGContent.ItemCloak, 1, 4), null, null, null});
            inv.func_70299_a(expanded, raven);
            Object[][] list = ActiveAbilityHandler.instance.buildActiveAbilityList(player);
            int movedId = -1, nativeId = -1;
            for (Object[] entry : list) {
                if (entry[0] == raven) movedId = (Integer) entry[1];
                else if (((ItemStack) entry[0]).func_77973_b() == WGContent.ItemCloak) nativeId = (Integer) entry[1];
            }
            c.check(movedId == LegacyWitchingBaubles.SLOT_BASE + expanded, "ability wheel uses unique expanded slot ID");
            c.check(nativeId == 17, "native TG ability keeps original protocol ID");
            MessageActiveAbility outgoing = new MessageActiveAbility(player, movedId), incoming = new MessageActiveAbility();
            ByteBuf bytes = Unpooled.buffer();
            try {
                outgoing.toBytes(bytes); incoming.fromBytes(bytes);
                c.check(BaubleSlotClientProbe.field(MessageActiveAbility.class, "slot").getInt(incoming) == movedId,
                    "native ability packet preserves extended slot ID");
            } finally { bytes.release(); }
            c.empty(player);
            ItemStack storage = new ItemStack(WGContent.ItemCloak, 1, 2);
            inv.func_70299_a(BaubleSlotChecks.slot("ring"), storage);
            Object gui = new witchinggadgets.client.ClientProxy().getClientGuiElement(4, player, player.field_70170_p,
                BaubleSlotChecks.slot("ring"), LegacyWitchingBaubles.GUI_MARKER, 0);
            c.check(gui instanceof witchinggadgets.client.gui.GuiCloakBag, "explicit moved storage GUI opens on client");
            c.check(new witchinggadgets.client.ClientProxy().getClientGuiElement(4, player, player.field_70170_p,
                999, LegacyWitchingBaubles.GUI_MARKER, 0) == null, "stale client storage slot rejected");
            c.empty(player);

            ItemStack jump = new ItemStack(WGContent.ItemMagicalBaubles, 1, 0);
            TravellersGearAPI.setExtendedInventory(player, new ItemStack[]{null, jump, null, null});
            WGKeyHandler key = new WGKeyHandler();
            boolean focus = mc.field_71415_G;
            try {
                mc.field_71415_G = true; player.field_70160_al = true;
                KeyBinding.func_74510_a(mc.field_71474_y.field_74314_A.func_151463_i(), true);
                key.playerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player));
                c.check(BaubleSlotClientProbe.field(WGKeyHandler.class, "multiJumps").getInt(key) == 1, "native TG jump charm remains functional");
            } finally {
                KeyBinding.func_74510_a(mc.field_71474_y.field_74314_A.func_151463_i(), false);
                mc.field_71415_G = focus;
            }
        } finally { c.empty(player); rule.undo(); TravellersGearAPI.setExtendedInventory(player, saved); }
    }
}
