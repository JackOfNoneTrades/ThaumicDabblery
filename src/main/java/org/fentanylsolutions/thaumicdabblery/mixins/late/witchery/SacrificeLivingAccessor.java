package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.entity.EntityLiving;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.emoniph.witchery.ritual.SacrificeLiving;

@Mixin(value = SacrificeLiving.class, remap = false)
public interface SacrificeLivingAccessor {

    @Accessor("entityLivingClass")
    Class<? extends EntityLiving> td$getEntityClass();
}
