package org.fentanylsolutions.thaumicdabblery.mixins.late.witchinggadgets.legacy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.LegacyWitchingBaubles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "witchinggadgets.common.util.WGKeyHandler", remap = false)
public abstract class MixinKeyHandlerBaubleSlots {

    @Redirect(
        method = "playerTick",
        at = @At(
            value = "INVOKE",
            target = "Ltravellersgear/api/TravellersGearAPI;getExtendedInventory(Lnet/minecraft/entity/player/EntityPlayer;)[Lnet/minecraft/item/ItemStack;"))
    private ItemStack[] td$includeMovedItem(EntityPlayer player) {
        return LegacyWitchingBaubles.magicInventory(player, 1, 0);
    }
}
