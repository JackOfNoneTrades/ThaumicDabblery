package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumichorizons;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;

import org.fentanylsolutions.thaumicdabblery.feature.effigyskins.EffigySkinHolder;
import org.fentanylsolutions.thaumicdabblery.feature.effigyskins.EffigySkins;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.kentington.thaumichorizons.common.tiles.TileVat;
import com.mojang.authlib.GameProfile;

@Mixin(value = TileVat.class, remap = false)
public abstract class MixinTileVatEffigySkin implements EffigySkinHolder {

    @Unique
    private GameProfile thaumicdabblery$effigyProfile;

    @Override
    public GameProfile thaumicdabblery$getEffigyProfile() {
        return thaumicdabblery$effigyProfile;
    }

    @Override
    public void thaumicdabblery$setEffigyProfile(GameProfile profile) {
        thaumicdabblery$effigyProfile = EffigySkins.copy(profile);
    }

    @Inject(method = "writeCustomNBT", at = @At("TAIL"), require = 1)
    private void thaumicdabblery$writeSkin(NBTTagCompound tag, CallbackInfo ci) {
        EffigySkins.write(this, tag);
    }

    @Inject(method = "readCustomNBT", at = @At("TAIL"), require = 1)
    private void thaumicdabblery$readSkin(NBTTagCompound tag, CallbackInfo ci) {
        EffigySkins.read(this, tag);
    }

    @Inject(method = { "updateEntity", "func_145845_h" }, at = @At("TAIL"), require = 1)
    private void thaumicdabblery$inheritSkin(CallbackInfo ci) {
        EffigySkins.inherit((TileVat) (Object) this);
    }

    @Inject(method = { "killSubject", "killMe" }, at = @At("HEAD"), require = 1)
    private void thaumicdabblery$clearSkin(CallbackInfo ci) {
        thaumicdabblery$effigyProfile = null;
    }

    @Inject(method = "setEntityContained", at = @At("HEAD"), require = 1)
    private void thaumicdabblery$consumeSkin(EntityLivingBase entity, CallbackInfo ci) {
        if (entity != null) thaumicdabblery$effigyProfile = null;
    }
}
