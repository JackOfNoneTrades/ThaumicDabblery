package org.fentanylsolutions.thaumicdabblery.mixins.late.salisarcana;

import net.minecraft.inventory.IInventory;

import org.fentanylsolutions.thaumicdabblery.compat.salisarcana.BraceletReplacementGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
    targets = { "dev.rndmorris.salisarcana.common.recipes.ReplaceWandCapsRecipe",
        "dev.rndmorris.salisarcana.common.recipes.ReplaceWandCoreRecipe" },
    remap = false)
public abstract class MixinWandReplacementBracelets {

    @Inject(method = "scanTable", at = @At("HEAD"), cancellable = true, require = 1)
    private void td$rejectBraceletInput(IInventory inventory, CallbackInfoReturnable<Object> cir) {
        // Only the crafting grid: a bracelet in the workbench's power slot must remain usable.
        for (int slot = 0; slot < Math.min(9, inventory.getSizeInventory()); slot++) {
            if (BraceletReplacementGuard.isBracelet(inventory.getStackInSlot(slot))) {
                cir.setReturnValue(null);
                return;
            }
        }
    }
}
