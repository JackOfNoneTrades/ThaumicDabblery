package org.fentanylsolutions.thaumicdabblery.mixins.late.gadomancy;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import makeo.gadomancy.api.AuraEffect;
import makeo.gadomancy.common.aura.AuraEffectHandler;

@Mixin(value = AuraEffectHandler.class, remap = false)
public interface AuraEffectHandlerInvoker {

    @Invoker("doEntityEffects")
    static void td$entities(AuraEffect effect, World world, double x, double y, double z) {
        throw new AssertionError();
    }

    @Invoker("doBlockEffects")
    static void td$blocks(AuraEffect effect, World world, double x, double y, double z) {
        throw new AssertionError();
    }
}
