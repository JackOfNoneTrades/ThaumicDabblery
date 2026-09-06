package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumicconcilium;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.wandcomponents.WandComponentStatsRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.ilya3point999k.thaumicconcilium.common.items.wands.Bracelets", remap = false)
public abstract class MixinBraceletCapacity {

    @Inject(method = "getMaxVis", at = @At("RETURN"), cancellable = true, require = 1)
    private void thaumicdabblery$capacity(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        int capacity = WandComponentStatsRegistry.capacity(stack, cir.getReturnValueI());
        if (capacity != cir.getReturnValueI()) cir.setReturnValue(capacity);
    }
}
