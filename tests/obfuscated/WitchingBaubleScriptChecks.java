package tdtest;

import java.util.Arrays;
import net.minecraft.item.ItemStack;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleRules;
import thaumcraft.common.config.ConfigItems;
import witchinggadgets.common.WGContent;
import witchinggadgets.common.items.baubles.ItemMagicalBaubles;

public final class WitchingBaubleScriptChecks {
    public static int sniperMetadata() {
        int meta = Arrays.asList(ItemMagicalBaubles.subNames).indexOf("ringSniper");
        if (meta < 0) throw new AssertionError("no registered sniper ring");
        return meta;
    }
    public static void run(BaubleSlotChecks c) {
        ItemStack[] stacks = {
            new ItemStack(WGContent.ItemMagicalBaubles, 1, 0),
            new ItemStack(WGContent.ItemMagicalBaubles, 1, 3),
            new ItemStack(WGContent.ItemMagicalBaubles, 1, sniperMetadata()),
            new ItemStack(WGContent.ItemCloak, 1, 2),
            new ItemStack(WGContent.ItemKama, 1, 2),
            new ItemStack(ConfigItems.itemFocusPouch)
        };
        String[] types = {"ring", "amulet", "belt", "ring", "amulet", "charm"};
        for (int i = 0; i < stacks.length; i++) {
            c.check(BaubleRules.hasRule(stacks[i]) && Arrays.equals(BaubleRules.types(stacks[i]), new String[]{types[i]}),
                "WG script fully applied for fixture " + i);
        }
        c.check(stacks[2].func_77973_b().func_77667_c(stacks[2]).endsWith("ringSniper"), "tested ring is the real registered sniper item");
    }
}
