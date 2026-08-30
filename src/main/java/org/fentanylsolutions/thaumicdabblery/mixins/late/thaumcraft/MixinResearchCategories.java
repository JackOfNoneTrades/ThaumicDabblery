package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import net.minecraft.util.ResourceLocation;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.ResearchTabOrderRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import thaumcraft.api.research.ResearchCategories;

@Mixin(value = ResearchCategories.class, remap = false)
public abstract class MixinResearchCategories {

    @Inject(method = "registerCategory", at = @At("RETURN"), require = 1)
    private static void thaumicdabblery$applyDeferredTabOrder(String categoryKey, ResourceLocation icon,
        ResourceLocation background, CallbackInfo ci) {
        ResearchTabOrderRegistry.onCategoryRegistered(categoryKey);
    }
}
