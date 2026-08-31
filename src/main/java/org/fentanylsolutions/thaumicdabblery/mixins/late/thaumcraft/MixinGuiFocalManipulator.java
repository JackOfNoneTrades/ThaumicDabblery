package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.focusupgrades.FocusUpgradeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.client.gui.GuiFocalManipulator;

@Mixin(value = GuiFocalManipulator.class, remap = false)
public abstract class MixinGuiFocalManipulator {

    @Redirect(
        method = "gatherInfo",
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/api/wands/ItemFocusBasic;getPossibleUpgradesByRank(Lnet/minecraft/item/ItemStack;I)"
                + "[Lthaumcraft/api/wands/FocusUpgradeType;"),
        require = 1)
    private FocusUpgradeType[] thaumicdabblery$getScriptedUpgrades(ItemFocusBasic focus, ItemStack stack, int tier) {
        return FocusUpgradeHooks.getAvailableUpgrades(focus, stack, tier);
    }

    @Redirect(
        method = "gatherInfo",
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/api/wands/ItemFocusBasic;canApplyUpgrade(Lnet/minecraft/item/ItemStack;"
                + "Lnet/minecraft/entity/player/EntityPlayer;Lthaumcraft/api/wands/FocusUpgradeType;I)Z"),
        require = 1)
    private boolean thaumicdabblery$acceptScriptedUpgrade(ItemFocusBasic focus, ItemStack stack, EntityPlayer player,
        FocusUpgradeType upgrade, int tier) {
        return FocusUpgradeHooks.canApplyUpgrade(focus, stack, player, upgrade, tier);
    }
}
