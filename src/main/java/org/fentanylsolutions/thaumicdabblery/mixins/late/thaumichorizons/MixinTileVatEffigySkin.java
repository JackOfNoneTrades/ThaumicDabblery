package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumichorizons;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.feature.effigyskins.EffigySkinHolder;
import org.fentanylsolutions.thaumicdabblery.feature.effigyskins.EffigySkinNetwork;
import org.fentanylsolutions.thaumicdabblery.feature.effigyskins.EffigySkins;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.kentington.thaumichorizons.common.tiles.TileVat;
import com.mojang.authlib.GameProfile;

@Mixin(value = TileVat.class, remap = false)
public abstract class MixinTileVatEffigySkin implements EffigySkinHolder {

    @Redirect(
        method = { "updateEntity", "func_145845_h" },
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/common/CommonProxy;blockSparkle(Lnet/minecraft/world/World;IIIII)V"),
        require = 4)
    private void thaumicdabblery$syncRevivalSparkles(thaumcraft.common.CommonProxy proxy, World world, int x, int y,
        int z, int color, int count) {
        // Keep local growing-creature effects. Revival runs on the server, whose particle proxy is a no-op.
        if (world.isRemote) proxy.blockSparkle(world, x, y, z, color, count);
        else EffigySkinNetwork.sparkle(world, x, y, z, color, count);
    }

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
