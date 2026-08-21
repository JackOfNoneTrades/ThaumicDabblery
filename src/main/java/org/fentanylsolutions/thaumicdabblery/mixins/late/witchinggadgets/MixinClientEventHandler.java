package org.fentanylsolutions.thaumicdabblery.mixins.late.witchinggadgets;

import net.minecraftforge.client.event.GuiOpenEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import thaumcraft.api.research.ResearchCategories;

/** Prevents Witching Gadgets from updating the background of a tab that scripts removed. */
@Pseudo
@Mixin(targets = "witchinggadgets.client.ClientEventHandler", remap = false)
public abstract class MixinClientEventHandler {

    @Inject(method = "onGuiOpen", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$skipMissingResearchCategory(GuiOpenEvent event, CallbackInfo ci) {
        if (ResearchCategories.getResearchList("WITCHGADG") == null) {
            ci.cancel();
        }
    }
}
