package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import java.util.EnumSet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.emoniph.witchery.ritual.Circle;
import com.emoniph.witchery.ritual.RiteRegistry;
import com.emoniph.witchery.ritual.RitualTraits;
import com.emoniph.witchery.ritual.Sacrifice;

@Mixin(value = RiteRegistry.Ritual.class, remap = false)
public interface RitualAccessor {

    @Accessor("initialSacrifice")
    Sacrifice td$getSacrifice();

    @Mutable
    @Accessor("initialSacrifice")
    void td$setSacrifice(Sacrifice value);

    @Accessor("circles")
    Circle[] td$getCircles();

    @Mutable
    @Accessor("circles")
    void td$setCircles(Circle[] value);

    @Accessor("traits")
    EnumSet<RitualTraits> td$getTraits();

    @Mutable
    @Accessor("traits")
    void td$setTraits(EnumSet<RitualTraits> value);
}
