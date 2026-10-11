package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import java.util.Hashtable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.emoniph.witchery.brewing.BrewItemKey;
import com.emoniph.witchery.brewing.WitcheryBrewRegistry;
import com.emoniph.witchery.brewing.action.BrewAction;

@Mixin(value = WitcheryBrewRegistry.class, remap = false)
public interface BrewRegistryAccessor {

    @Accessor("ingredients")
    Hashtable<BrewItemKey, BrewAction> td$getIngredients();
}
