package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.feature.wandcomponents.WandComponentStatsFeature;
import org.fentanylsolutions.thaumicdabblery.feature.wandcomponents.WandComponentStatsRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import thaumcraft.api.wands.IWandRodOnUpdate;
import thaumcraft.common.items.wands.ItemWandCasting;

@Mixin(ItemWandCasting.class)
public abstract class MixinItemWandCastingStats {

    @Inject(method = "getMaxVis", at = @At("RETURN"), cancellable = true, remap = false, require = 1)
    private void thaumicdabblery$capacity(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        int capacity = WandComponentStatsRegistry.capacity(stack, cir.getReturnValueI());
        if (capacity != cir.getReturnValueI()) cir.setReturnValue(capacity);
    }

    @Inject(method = "getFocusPotency", at = @At("RETURN"), cancellable = true, remap = false, require = 1)
    private void thaumicdabblery$potency(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (!WandComponentStatsFeature.isEnabled()) return;
        ItemWandCasting wand = (ItemWandCasting) (Object) this;
        Integer levels = WandComponentStatsRegistry.getPotency(stack);
        if (levels != null && wand.getFocus(stack) != null) {
            cir.setReturnValue(cir.getReturnValueI() - (wand.hasRunes(stack) ? 1 : 0) + levels);
        }
    }

    @Redirect(
        method = "onUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/api/wands/IWandRodOnUpdate;onUpdate(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;)V",
            remap = false),
        require = 1)
    private void thaumicdabblery$nativeRegeneration(IWandRodOnUpdate update, ItemStack stack, EntityPlayer player) {
        WandComponentStatsRegistry.updateNative(update, stack, player);
    }

    @Inject(method = "onUpdate", at = @At("RETURN"), require = 1)
    private void thaumicdabblery$regeneration(ItemStack stack, World world, Entity entity, int slot, boolean held,
        CallbackInfo ci) {
        if (!world.isRemote && entity instanceof EntityPlayer) {
            WandComponentStatsRegistry.tick(stack, (EntityPlayer) entity);
        }
    }
}
