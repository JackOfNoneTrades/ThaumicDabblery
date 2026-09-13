package org.fentanylsolutions.thaumicdabblery.mixins.late.baubles;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleHooks;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import baubles.api.expanded.BaubleItemHelper;

@Mixin(value = BaubleItemHelper.class, remap = false)
public abstract class MixinBaubleItemHelper {

    @Inject(method = "onBaubleRightClick", at = @At("HEAD"), cancellable = true)
    private static void td$equipOnce(ItemStack stack, World world, EntityPlayer player,
        CallbackInfoReturnable<ItemStack> cir) {
        if (BaubleRules.hasRule(stack)) cir.setReturnValue(BaubleHooks.rightClick(stack, world, player));
    }
}
