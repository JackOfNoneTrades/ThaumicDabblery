package org.fentanylsolutions.thaumicdabblery.mixins.late.witchinggadgets.legacy;

import net.minecraft.entity.player.EntityPlayer;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.LegacyWitchingBaubles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "travellersgear.client.handlers.ActiveAbilityHandler", remap = false)
public abstract class MixinTravellersAbilityList {

    @Inject(method = "buildActiveAbilityList", at = @At("RETURN"))
    private void td$expandedIds(EntityPlayer player, CallbackInfoReturnable<Object[][]> cir) {
        LegacyWitchingBaubles.encodeSlots(player, cir.getReturnValue());
    }
}
