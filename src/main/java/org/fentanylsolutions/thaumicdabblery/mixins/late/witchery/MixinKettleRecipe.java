package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.feature.witcherykettle.KettleRegistry;
import org.fentanylsolutions.thaumicdabblery.feature.witcherykettle.WitcheryKettleFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.crafting.KettleRecipes.KettleRecipe;

@Mixin(value = KettleRecipe.class, remap = false)
public abstract class MixinKettleRecipe {

    @Inject(method = "isMatch", at = @At("HEAD"), cancellable = true, require = 1)
    private void td$dimension(ItemStack[] inputs, int length, boolean partial, World world,
        CallbackInfoReturnable<Boolean> cir) {
        KettleRegistry.Recipe r = KettleRegistry.definition((KettleRecipe) (Object) this);
        if (r != null && r.changed && r.dimension != null && r.dimension != world.provider.dimensionId)
            cir.setReturnValue(false);
    }

    @Inject(method = "getDescription", at = @At("RETURN"), cancellable = true, require = 1)
    private void td$requirements(CallbackInfoReturnable<String> cir) {
        if (!WitcheryKettleFeature.isEnabled()) return;
        String text = cir.getReturnValue();
        for (String line : KettleRegistry.requirements((KettleRecipe) (Object) this)) text += "\n" + line;
        cir.setReturnValue(text);
    }
}
