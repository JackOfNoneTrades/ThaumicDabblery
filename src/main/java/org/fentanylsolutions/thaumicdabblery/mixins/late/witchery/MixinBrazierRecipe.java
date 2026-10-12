package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.feature.witcherybrazier.BrazierRegistry;
import org.fentanylsolutions.thaumicdabblery.feature.witcherybrazier.BrazierRegistry.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.blocks.BlockBrazier.TileEntityBrazier;
import com.emoniph.witchery.crafting.BrazierRecipes.BrazierRecipe;

@Mixin(value = BrazierRecipe.class, remap = false)
public abstract class MixinBrazierRecipe {

    @Inject(method = "getNeedsPower", at = @At("HEAD"), cancellable = true, require = 1)
    private void td$power(CallbackInfoReturnable<Boolean> cir) {
        Recipe r = BrazierRegistry.definition((BrazierRecipe) (Object) this);
        if (r != null && r.changed) cir.setReturnValue(r.power > 0);
    }

    @Inject(method = "onBurning", at = @At("HEAD"), cancellable = true, require = 1)
    private void td$burn(World world, int x, int y, int z, long ticks, TileEntityBrazier tile,
        CallbackInfoReturnable<Integer> cir) {
        Recipe r = BrazierRegistry.definition((BrazierRecipe) (Object) this);
        if (r != null && r.changed)
            cir.setReturnValue(r.effect == null ? 0 : r.effect.onBurning(world, x, y, z, ticks, tile));
    }

    @Inject(method = "onBurnt", at = @At("HEAD"), cancellable = true, require = 1)
    private void td$finish(World world, int x, int y, int z, long ticks, TileEntityBrazier tile, CallbackInfo ci) {
        Recipe r = BrazierRegistry.definition((BrazierRecipe) (Object) this);
        if (r != null && r.changed) {
            if (r.entity != null) BrazierRegistry.summon(r, world, x, y, z);
            else r.effect.onBurnt(world, x, y, z, ticks, tile);
            ci.cancel();
        }
    }

    @Inject(method = "getDescription", at = @At("RETURN"), cancellable = true, require = 1)
    private void td$book(CallbackInfoReturnable<String> cir) {
        Recipe r = BrazierRegistry.definition((BrazierRecipe) (Object) this);
        if (r != null && r.changed) cir.setReturnValue(BrazierRegistry.bookText(r, cir.getReturnValue()));
    }
}
