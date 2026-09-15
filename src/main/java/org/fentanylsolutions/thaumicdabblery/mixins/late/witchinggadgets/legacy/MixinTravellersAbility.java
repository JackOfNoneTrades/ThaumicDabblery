package org.fentanylsolutions.thaumicdabblery.mixins.late.witchinggadgets.legacy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.LegacyWitchingBaubles;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.WitchingBaubleSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "travellersgear.common.network.MessageActiveAbility", remap = false)
public abstract class MixinTravellersAbility {

    @Shadow
    private static void activateItem(ItemStack stack, EntityPlayer player, boolean held) {}

    @Inject(method = "performAbility", at = @At("HEAD"), cancellable = true)
    private static void td$activateMovedCloak(EntityPlayer player, int encoded, CallbackInfo ci) {
        if (encoded < LegacyWitchingBaubles.SLOT_BASE) return;
        ci.cancel();
        ItemStack stack = LegacyWitchingBaubles.encodedStack(player, encoded);
        if (stack != null) {
            activateItem(stack, player, false);
            WitchingBaubleSlots.sync(player, stack);
        }
    }
}
