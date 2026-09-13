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

public final class BaubleSlotChecks {
    public int checks;
    public static NativeBauble nativeItem;
    public static void register() { nativeItem = new NativeBauble(); GameRegistry.registerItem(nativeItem, "native"); }
    public void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
    public ItemStack dirt() { return new ItemStack(Blocks.field_150346_d, 1, 0); }
    public void scripted() {
        check(Boolean.FALSE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment")), "obfuscated runtime");
        check(new HashSet<>(Arrays.asList(BaubleRules.types(dirt()))).equals(new HashSet<>(Arrays.asList("ring", "amulet"))), "dirt multislot script");
        check(BaubleRules.types(new ItemStack(Items.field_151055_y)).length == 0, "remove overrides previous add");
        check(Arrays.equals(BaubleRules.types(new ItemStack(Items.field_151045_i)), new String[]{"amulet"}), "add and remove individual types");
        check(Arrays.equals(BaubleRules.types(new ItemStack(Blocks.field_150325_L, 1, 0)), new String[]{"ring"}), "wildcard");
        check(Arrays.equals(BaubleRules.types(new ItemStack(Blocks.field_150325_L, 1, 14)), new String[]{"amulet"}), "exact overrides wildcard");
    }
    public void validation() throws Exception {
        String[] before = BaubleRules.types(dirt());
        for (String type : new String[]{null, "", "unknown", "not_a_slot"}) {
            try { BaubleRules.edit(dirt(), 0, new String[]{type}); throw new AssertionError("accepted " + type); }
            catch (IllegalArgumentException expected) { checks++; }
        }
        ItemStack tagged = dirt(); tagged.func_77982_d(new NBTTagCompound());
        try { BaubleRules.edit(tagged, 0, new String[]{"ring"}); throw new AssertionError("accepted NBT rule"); }
        catch (IllegalArgumentException expected) { checks++; }
        check(Arrays.equals(before, BaubleRules.types(dirt())), "invalid edits do not mutate");
        BaubleRules.Change edit = BaubleRules.edit(dirt(), 0, new String[]{" BELT ", "belt"});
        check(Arrays.equals(BaubleRules.types(dirt()), new String[]{"belt"}), "normalization and deduplication");
        edit.undo();
        check(Arrays.equals(before, BaubleRules.types(dirt())), "undo restores preceding rule");
        ItemStack tool=new ItemStack(Items.field_151040_l,1,1);
        BaubleRules.Change toolEdit=BaubleRules.edit(tool,0,new String[]{"ring"});
        tool.func_77964_b(25);
        check(Arrays.equals(BaubleRules.types(tool),new String[]{"ring"}),"damageable rule survives durability changes");
        toolEdit.undo();
        java.lang.reflect.Field enabled=BaubleSlotsFeature.class.getDeclaredField("enabled");
        enabled.setAccessible(true);
        boolean oldEnabled=enabled.getBoolean(null);
        try {
            enabled.setBoolean(null,false);
            check(!BaubleRules.hasRule(dirt()) && BaubleRules.types(dirt()).length==0,"feature disable restores ordinary item behavior");
            check(Arrays.equals(BaubleRules.types(new ItemStack(nativeItem)),new String[]{"ring"}),"feature disable preserves native types");
        } finally { enabled.setBoolean(null,oldEnabled); }
        check(Arrays.equals(before,BaubleRules.types(dirt())),"feature re-enable preserves scripted rules");
    }
    public static int slot(String type) { return BaubleExpandedSlots.getIndexesOfAssignedSlotsOfType(type)[0]; }
    public void empty(EntityPlayer player) {
        InventoryBaubles inv = (InventoryBaubles) BaublesApi.getBaubles(player);
        inv.blockEvents = false;
        for (int i = 0; i < inv.func_70302_i_(); i++) if (inv.func_70301_a(i) != null) inv.func_70299_a(i, null);
        Arrays.fill(player.field_71071_by.field_70462_a, null);
        player.field_71071_by.func_70437_b(null);
        player.field_71071_by.field_70461_c = 0;
    }
    public void inventory(EntityPlayer player) {
        empty(player);
        InventoryBaubles inv = (InventoryBaubles) BaublesApi.getBaubles(player);
        ContainerPlayerExpanded container = new ContainerPlayerExpanded(player.field_71071_by, false, player);
        int ring = slot("ring"), amulet = slot("amulet"), belt = slot("belt");
        for (int i = 0; i < inv.func_70302_i_(); i++) {
            boolean expected = BaubleHooks.fits(dirt(), BaubleExpandedSlots.getSlotType(i));
            check(inv.func_94041_b(i, dirt()) == expected, "inventory validity " + i);
            check(container.getBaubleSlot(i).func_75214_a(dirt()) == expected, "slot validity " + i);
        }
        ItemStack stack = dirt(); stack.field_77994_a = 64;
        NBTTagCompound tag = new NBTTagCompound(); tag.func_74778_a("probe", "unchanged"); stack.func_77982_d(tag);
        player.field_71071_by.func_70437_b(stack);
        Slot target = container.getBaubleSlot(ring);
        container.func_75144_a(target.field_75222_d, 0, 0, player);
        check(inv.func_70301_a(ring).field_77994_a == 1 && player.field_71071_by.func_70445_o().field_77994_a == 63, "normal click splits one");
        check(inv.func_70301_a(ring).func_77973_b() == Item.func_150898_a(Blocks.field_150346_d), "real item identity retained");
        check(inv.func_70301_a(ring).func_77978_p().func_74779_i("probe").equals("unchanged"), "NBT preserved");
        check(target.func_82869_a(player), "ordinary item can be removed");
        player.field_71071_by.field_70462_a[1] = player.field_71071_by.func_70445_o();
        player.field_71071_by.func_70437_b(null);
        container.func_75144_a(target.field_75222_d, 0, 0, player);
        check(inv.func_70301_a(ring) == null && player.field_71071_by.func_70445_o().field_77994_a == 1
            && player.field_71071_by.field_70462_a[1].field_77994_a == 63, "take out without loss");
        player.field_71071_by.field_70462_a[1] = null;
        player.field_71071_by.func_70437_b(null);
        player.field_71071_by.field_70462_a[0] = new ItemStack(Blocks.field_150346_d, 64);
        int mainSlot = -1;
        for (Object object : container.field_75151_b) {
            Slot s = (Slot) object;
            if (s.field_75224_c == player.field_71071_by && s.func_75211_c() == player.field_71071_by.field_70462_a[0]) mainSlot = s.field_75222_d;
        }
        check(mainSlot >= 0, "find player slot");
        container.func_82846_b(player, mainSlot);
        int equipped = 0;
        for (int i = 0; i < inv.func_70302_i_(); i++) if (inv.func_70301_a(i) != null) {
            equipped += inv.func_70301_a(i).field_77994_a;
            check(inv.func_70301_a(i).field_77994_a == 1 && inv.func_94041_b(i, inv.func_70301_a(i)), "shift-click respects slot/count");
        }
        check(equipped >= 3 && equipped + player.field_71071_by.field_70462_a[0].field_77994_a == 64, "shift-click count conservation");
        container.func_82846_b(player, target.field_75222_d);
        check(inv.func_70301_a(ring) == null, "shift-click removal");
        empty(player);
        ItemStack nativeStack = new ItemStack(nativeItem);
        BaubleRules.Change moved = BaubleRules.edit(nativeStack, 0, new String[]{"amulet"});
        nativeItem.equipped = nativeItem.unequipped = nativeItem.ticked = 0;
        check(!inv.func_94041_b(ring, nativeStack) && inv.func_94041_b(amulet, nativeStack), "native ring moved to amulet");
        nativeItem.allowEquip = false;
        check(!inv.func_94041_b(amulet, nativeStack), "native equip restriction preserved");
        nativeItem.allowEquip = true;
        inv.func_70299_a(amulet, nativeStack);
        check(nativeItem.equipped == 1, "one equip callback");
        new baubles.common.event.EventHandlerEntity().playerTick(new LivingEvent.LivingUpdateEvent(player));
        check(nativeItem.ticked == 1, "native worn callback");
        nativeItem.allowUnequip = false;
        check(!container.getBaubleSlot(amulet).func_82869_a(player), "native unequip restriction preserved");
        nativeItem.allowUnequip = true;
        inv.func_70299_a(amulet, null);
        check(nativeItem.unequipped == 1, "one removal callback");
        player.field_71075_bZ.field_75098_d = false;
        player.field_71071_by.field_70462_a[0] = nativeStack;
        BaubleItemHelper.onBaubleRightClick(nativeStack, player.field_70170_p, player);
        check(nativeItem.equipped == 2, "right click does not duplicate equip callback");
        check(inv.func_70301_a(amulet) != null && player.field_71071_by.field_70462_a[0] == null, "right click uses new slot");
        empty(player); moved.undo();
        check(inv.func_94041_b(ring, new ItemStack(nativeItem)), "native type restored");
        inv.func_70299_a(ring, dirt());
        BaubleReconciler reconciler = new BaubleReconciler();
        reconciler.reconcile(player);
        NBTTagCompound saved = new NBTTagCompound();
        inv.saveNBT(saved);
        InventoryBaubles loaded = new InventoryBaubles(player); loaded.readNBT(saved);
        check(loaded.func_70301_a(ring).func_77973_b() == dirt().func_77973_b(), "bauble NBT roundtrip");
        BaubleRules.Change denied = BaubleRules.edit(dirt(), 0, new String[0]);
        for (int i = 0; i < player.field_71071_by.field_70462_a.length; i++) player.field_71071_by.field_70462_a[i] = new ItemStack(Blocks.field_150348_b, 64);
        reconciler.reconcile(player);
        check(inv.func_70301_a(ring) != null && container.getBaubleSlot(ring).func_82869_a(player), "full inventory retains recoverable item");
        player.field_71071_by.field_70462_a[0] = null;
        reconciler.reconcile(player);
        check(inv.func_70301_a(ring) == null && player.field_71071_by.field_70462_a[0].func_77973_b() == dirt().func_77973_b(), "return disallowed item once room exists");
        denied.undo(); empty(player);
    }
    public static final class NativeBauble extends Item implements IBaubleExpanded {
        public int equipped, unequipped, ticked;
        public boolean allowEquip = true, allowUnequip = true;
        NativeBauble() { func_77625_d(1); func_77655_b("td_native_bauble"); }
        public BaubleType getBaubleType(ItemStack s) { return BaubleType.RING; }
        public String[] getBaubleTypes(ItemStack s) { return new String[]{"ring"}; }
        public boolean canEquip(ItemStack s, EntityLivingBase p) { return allowEquip; }
        public boolean canUnequip(ItemStack s, EntityLivingBase p) { return allowUnequip; }
        public void onEquipped(ItemStack s, EntityLivingBase p) { equipped++; }
        public void onUnequipped(ItemStack s, EntityLivingBase p) { unequipped++; }
        public void onWornTick(ItemStack s, EntityLivingBase p) { ticked++; }
    }
}
