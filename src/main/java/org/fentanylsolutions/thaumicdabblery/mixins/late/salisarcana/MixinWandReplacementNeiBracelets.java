package org.fentanylsolutions.thaumicdabblery.mixins.late.salisarcana;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.compat.salisarcana.BraceletReplacementGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
    targets = { "dev.rndmorris.salisarcana.common.compat.nei.WandCapSubstitutionHandler",
        "dev.rndmorris.salisarcana.common.compat.nei.WandCoreSubstitutionHandler" },
    remap = false)
public abstract class MixinWandReplacementNeiBracelets {

    @Inject(
        method = "loadUsageRecipes(Lnet/minecraft/item/ItemStack;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private void td$skipBraceletRecipes(ItemStack ingredient, CallbackInfo ci) {
        if (BraceletReplacementGuard.isBracelet(ingredient)) ci.cancel();
    }
}
