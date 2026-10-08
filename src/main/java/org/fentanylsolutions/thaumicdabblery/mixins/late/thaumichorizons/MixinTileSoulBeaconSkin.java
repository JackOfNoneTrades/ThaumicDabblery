package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumichorizons;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import org.fentanylsolutions.thaumicdabblery.feature.effigyskins.EffigySkinHolder;
import org.fentanylsolutions.thaumicdabblery.feature.effigyskins.EffigySkins;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.kentington.thaumichorizons.common.tiles.TileSoulBeacon;
import com.mojang.authlib.GameProfile;

import thaumcraft.api.TileThaumcraft;

@Mixin(value = TileSoulBeacon.class, remap = false)
public abstract class MixinTileSoulBeaconSkin extends TileThaumcraft implements EffigySkinHolder {

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

    @Override
    public void writeCustomNBT(NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        EffigySkins.write(this, tag);
    }

    @Override
    public void readCustomNBT(NBTTagCompound tag) {
        super.readCustomNBT(tag);
        EffigySkins.read(this, tag);
    }

    @Inject(method = "activate", at = @At("RETURN"), require = 1)
    private void thaumicdabblery$bindSkin(EntityPlayer player, CallbackInfoReturnable<Boolean> ci) {
        if (ci.getReturnValue()) EffigySkins.bind((TileSoulBeacon) (Object) this, player);
    }
}
