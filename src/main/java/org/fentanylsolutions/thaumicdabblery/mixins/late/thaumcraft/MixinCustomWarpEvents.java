package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.util.FakePlayer;

import org.fentanylsolutions.thaumicdabblery.feature.warpevents.CustomWarpEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import thaumcraft.common.lib.WarpEvents;

@Mixin(value = WarpEvents.class, remap = false)
public abstract class MixinCustomWarpEvents {

    @Inject(
        method = "checkWarpEvent",
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/common/lib/research/PlayerKnowledge;getWarpCounter(Ljava/lang/String;)I"),
        require = 1)
    private static void td$captureTotal(EntityPlayer player, CallbackInfo ci, @Local(index = 1) int warp,
        @Share("td$total") LocalIntRef total) {
        total.set(warp);
    }

    @Inject(
        method = "checkWarpEvent",
        at = @At(
            value = "INVOKE",
            target = "Lcpw/mods/fml/common/network/simpleimpl/SimpleNetworkWrapper;sendTo(Lcpw/mods/fml/common/network/simpleimpl/IMessage;Lnet/minecraft/entity/player/EntityPlayerMP;)V",
            ordinal = 0),
        require = 1)
    private static void td$select(EntityPlayer player, CallbackInfo ci, @Local(index = 5) LocalIntRef effect,
        @Share("td$total") LocalIntRef total, @Share("td$event") LocalRef<CustomWarpEvents.Event> selected) {
        if (!(player instanceof EntityPlayerMP) || player instanceof FakePlayer || player.worldObj.isRemote) return;
        CustomWarpEvents.Event event = CustomWarpEvents.select(total.get(), player.worldObj.rand);
        if (event != null) {
            selected.set(event);
            // Skip only the native effect. Keep native research unlocks, counter decay and synchronization.
            effect.set(0);
        }
    }

    @Inject(method = "checkWarpEvent", at = @At("RETURN"), require = 1)
    private static void td$execute(EntityPlayer player, CallbackInfo ci,
        @Share("td$event") LocalRef<CustomWarpEvents.Event> selected) {
        // Commands may teleport the player or reload scripts, so run after native bookkeeping finishes.
        if (selected.get() != null) CustomWarpEvents.execute(selected.get(), (EntityPlayerMP) player);
    }
}
