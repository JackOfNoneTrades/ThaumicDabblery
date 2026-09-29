package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.wands.WandRodPrimalOnUpdate;

@Mixin(value = WandRodPrimalOnUpdate.class, remap = false)
public abstract class MixinPrimalRod {

    @Redirect(
        method = "onUpdate",
        at = @At(
            value = "FIELD",
            target = "Lthaumcraft/common/items/wands/WandRodPrimalOnUpdate;primals:Ljava/util/ArrayList;",
            opcode = 180))
    private ArrayList<Aspect> td$currentPrimals(WandRodPrimalOnUpdate rod) {
        return Aspect.getPrimalAspects();
    }
}
