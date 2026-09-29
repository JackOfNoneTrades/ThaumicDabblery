package org.fentanylsolutions.thaumicdabblery.mixins.late.gadomancy;

import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.feature.aurapylon.AuraPylonRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import makeo.gadomancy.common.aura.AuraEffectHandler;
import thaumcraft.api.aspects.Aspect;

@Mixin(value = AuraEffectHandler.class, remap = false)
public abstract class MixinAuraEffectHandler {

    @Inject(method = "distributeEffects", at = @At("HEAD"), cancellable = true, require = 1)
    private static void td$dispatch(Aspect aspect, World world, double x, double y, double z, int tick,
        CallbackInfo ci) {
        if (AuraPylonRegistry.distribute(aspect, world, x, y, z, tick)) ci.cancel();
    }
}
