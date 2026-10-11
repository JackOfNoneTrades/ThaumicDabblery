package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.witcherydistillery.DistilleryRegistry;
import org.fentanylsolutions.thaumicdabblery.feature.witcherydistillery.WitcheryDistilleryFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.crafting.DistilleryRecipes.DistilleryRecipe;

@Mixin(value = DistilleryRecipe.class, remap = false)
public abstract class MixinDistilleryBook {

    @Inject(method = "getDescription", at = @At("HEAD"), cancellable = true, require = 1)
    private void td$counts(CallbackInfoReturnable<String> cir) {
        DistilleryRegistry.Recipe r = DistilleryRegistry.definition((DistilleryRecipe) (Object) this);
        if (!WitcheryDistilleryFeature.isEnabled() || r == null || !r.changed) return;
        StringBuilder text = new StringBuilder(Witchery.resource("witchery.book.distillery.items")).append("\n\n");
        for (ItemStack s : r.inputs) if (s != null) text.append("> ")
            .append(s.getDisplayName())
            .append("\n");
        text.append("\n")
            .append(Witchery.resource("witchery.book.distillery.jars"))
            .append(" ")
            .append(r.jars)
            .append("\n\n");
        text.append(Witchery.resource("witchery.book.distillery.results"))
            .append("\n\n");
        for (ItemStack s : r.outputs) if (s != null) text.append("> ")
            .append(s.stackSize)
            .append(" x ")
            .append(s.getDisplayName())
            .append("\n");
        cir.setReturnValue(text.toString());
    }
}
