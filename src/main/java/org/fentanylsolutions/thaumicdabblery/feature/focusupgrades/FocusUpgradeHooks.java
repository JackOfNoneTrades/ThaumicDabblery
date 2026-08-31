package org.fentanylsolutions.thaumicdabblery.feature.focusupgrades;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;

public final class FocusUpgradeHooks {

    private FocusUpgradeHooks() {}

    public static FocusUpgradeType[] getAvailableUpgrades(ItemFocusBasic focus, ItemStack stack, int tier) {
        FocusUpgradeType[] nativeUpgrades = focus.getPossibleUpgradesByRank(stack, tier);
        if (!FocusUpgradesFeature.isEnabled()) {
            return nativeUpgrades;
        }
        return FocusUpgradeRegistry.getAvailableUpgrades(stack, tier, nativeUpgrades);
    }

    public static boolean canApplyUpgrade(ItemFocusBasic focus, ItemStack stack, EntityPlayer player,
        FocusUpgradeType upgrade, int tier) {
        if (FocusUpgradesFeature.isEnabled() && FocusUpgradeRegistry.isExplicitlyAdded(stack, tier, upgrade)) {
            return true;
        }
        return focus.canApplyUpgrade(stack, player, upgrade, tier);
    }
}
