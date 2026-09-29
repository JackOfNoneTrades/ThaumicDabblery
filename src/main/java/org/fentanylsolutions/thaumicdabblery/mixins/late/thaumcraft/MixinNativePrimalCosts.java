package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import java.util.ArrayList;

import org.fentanylsolutions.thaumicdabblery.feature.customaspects.CustomAspectRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ConfigResearch;
import thaumcraft.common.lib.crafting.ArcaneSceptreRecipe;
import thaumcraft.common.lib.crafting.ArcaneWandRecipe;

@Mixin(value = { ArcaneWandRecipe.class, ArcaneSceptreRecipe.class, ConfigResearch.class }, remap = false)
public abstract class MixinNativePrimalCosts {

    @Redirect(
        method = "*",
        at = @At(value = "INVOKE", target = "Lthaumcraft/api/aspects/Aspect;getPrimalAspects()Ljava/util/ArrayList;"),
        require = 1)
    private static ArrayList<Aspect> td$nativeCosts() {
        ArrayList<Aspect> primals = Aspect.getPrimalAspects();
        primals.removeIf(CustomAspectRegistry::isCustomPrimal);
        return primals;
    }
}
