package org.fentanylsolutions.thaumicdabblery.mixins.early;

import java.util.Objects;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;

import org.fentanylsolutions.thaumicdabblery.feature.planarvortex.VortexSuction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep vortex output immunity attached to its items when vanilla merges dropped stacks. */
@Mixin(EntityItem.class)
public abstract class MixinEntityItemVortexOutput implements VortexSuction.Holder {

    @Unique
    private VortexSuction.Path thaumicdabblery$path;
    @Unique
    private boolean thaumicdabblery$moving, thaumicdabblery$oldNoClip;

    public VortexSuction.Path thaumicdabblery$suction() {
        return thaumicdabblery$path;
    }

    public void thaumicdabblery$suction(VortexSuction.Path path) {
        thaumicdabblery$path = path;
    }

    @Inject(method = "onUpdate", at = @At("HEAD"), require = 1)
    private void thaumicdabblery$beforeFlight(CallbackInfo ci) {
        EntityItem self = (EntityItem) (Object) this;
        thaumicdabblery$moving = VortexSuction.active(self) != null;
        if (!thaumicdabblery$moving) return;
        thaumicdabblery$oldNoClip = self.noClip;
        self.noClip = true;
        self.motionX = self.motionZ = 0;
        self.motionY = .03999999910593033D; // Cancel vanilla item gravity during the controlled flight.
    }

    @Inject(method = "onUpdate", at = @At("RETURN"), require = 1)
    private void thaumicdabblery$afterFlight(CallbackInfo ci) {
        if (!thaumicdabblery$moving) return;
        EntityItem self = (EntityItem) (Object) this;
        self.noClip = thaumicdabblery$oldNoClip;
        VortexSuction.Path path = VortexSuction.active(self);
        if (path != null) path.move(self);
        thaumicdabblery$moving = false;
    }

    @Inject(method = "onCollideWithPlayer", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$holdOffering(EntityPlayer player, CallbackInfo ci) {
        if (VortexSuction.active((EntityItem) (Object) this) != null) ci.cancel();
    }

    @Inject(method = "combineItems", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$keepOutputSeparate(EntityItem other, CallbackInfoReturnable<Boolean> cir) {
        EntityItem self = (EntityItem) (Object) this;
        if (VortexSuction.active(self) != null || VortexSuction.active(other) != null
            || !Objects.equals(
                self.getEntityData()
                    .getTag("thaumicdabblery:vortexOutput"),
                other.getEntityData()
                    .getTag("thaumicdabblery:vortexOutput")))
            cir.setReturnValue(false);
    }
}
