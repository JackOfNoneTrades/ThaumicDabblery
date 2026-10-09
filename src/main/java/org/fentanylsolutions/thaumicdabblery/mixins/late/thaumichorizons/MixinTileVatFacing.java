package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumichorizons;

import net.minecraft.nbt.NBTTagCompound;

import org.fentanylsolutions.thaumicdabblery.feature.vatfacing.VatFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.kentington.thaumichorizons.common.tiles.TileVat;

@Mixin(value = TileVat.class, remap = false)
public abstract class MixinTileVatFacing implements VatFacing.Holder {

    @Unique
    private final VatFacing.State thaumicdabblery$facing = new VatFacing.State();

    public VatFacing.State thaumicdabblery$facing() {
        return thaumicdabblery$facing;
    }

    @Inject(method = { "updateEntity", "func_145845_h" }, at = @At("RETURN"), require = 1)
    private void thaumicdabblery$updateFacing(CallbackInfo ci) {
        VatFacing.tick((TileVat) (Object) this);
    }

    @Inject(method = "writeCustomNBT", at = @At("TAIL"), require = 1)
    private void thaumicdabblery$writeFacing(NBTTagCompound tag, CallbackInfo ci) {
        thaumicdabblery$facing.write(tag);
    }

    @Inject(method = "readCustomNBT", at = @At("TAIL"), require = 1)
    private void thaumicdabblery$readFacing(NBTTagCompound tag, CallbackInfo ci) {
        thaumicdabblery$facing.read(tag);
    }
}
