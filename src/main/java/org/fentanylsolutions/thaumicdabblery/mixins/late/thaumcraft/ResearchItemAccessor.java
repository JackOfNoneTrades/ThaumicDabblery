package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import thaumcraft.api.research.ResearchItem;

@Mixin(value = ResearchItem.class, remap = false)
public interface ResearchItemAccessor {

    @Accessor("isHidden")
    boolean thaumicdabblery$isHidden();

    @Accessor("isHidden")
    void thaumicdabblery$setHidden(boolean hidden);

    @Accessor("isLost")
    boolean thaumicdabblery$isLost();

    @Accessor("isLost")
    void thaumicdabblery$setLost(boolean lost);
}
