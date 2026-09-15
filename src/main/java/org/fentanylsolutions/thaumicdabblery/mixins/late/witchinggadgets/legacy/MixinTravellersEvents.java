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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import cpw.mods.fml.common.eventhandler.Event;

@Mixin(targets = "travellersgear.common.util.TGEventHandler", remap = false)
public abstract class MixinTravellersEvents {

    @Shadow
    private static void triggerItemEvent(ItemStack stack, EntityPlayer player, boolean held, Event event) {}

    @Inject(method = "buildEventGearList", at = @At("RETURN"))
    private void td$expandedIds(EntityPlayer player, CallbackInfoReturnable<Object[][]> cir) {
        LegacyWitchingBaubles.encodeSlots(player, cir.getReturnValue());
    }

    @Inject(method = "triggerGear", at = @At("HEAD"), cancellable = true)
    private static void td$triggerMovedCloak(EntityPlayer player, int encoded, Event event, CallbackInfo ci) {
        if (encoded < LegacyWitchingBaubles.SLOT_BASE) return;
        ci.cancel();
        ItemStack stack = LegacyWitchingBaubles.encodedStack(player, encoded);
        if (stack != null) {
            triggerItemEvent(stack, player, false, event);
            WitchingBaubleSlots.sync(player, stack);
        }
    }
}
