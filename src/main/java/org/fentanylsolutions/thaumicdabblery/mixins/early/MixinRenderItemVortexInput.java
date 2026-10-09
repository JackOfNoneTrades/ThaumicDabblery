package org.fentanylsolutions.thaumicdabblery.mixins.early;

import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.item.EntityItem;

import org.fentanylsolutions.thaumicdabblery.feature.planarvortex.VortexSuction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderItem.class)
public abstract class MixinRenderItemVortexInput {

    @Inject(
        method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private void thaumicdabblery$absorbed(EntityItem item, double x, double y, double z, float yaw, float partialTicks,
        CallbackInfo ci) {
        if (VortexSuction.absorbed(item)) ci.cancel();
    }
}
