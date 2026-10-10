package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.emoniph.witchery.ritual.Sacrifice;
import com.emoniph.witchery.ritual.SacrificeMultiple;

@Mixin(value = SacrificeMultiple.class, remap = false)
public interface SacrificeMultipleAccessor {

    @Accessor("sacrifices")
    Sacrifice[] td$getSacrifices();
}
