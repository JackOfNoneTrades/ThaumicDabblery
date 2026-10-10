package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumichorizons;

import org.fentanylsolutions.thaumicdabblery.feature.planarvortex.VortexPage;
import org.fentanylsolutions.thaumicdabblery.feature.planarvortex.VortexPageRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import thaumcraft.api.research.ResearchPage;
import thaumcraft.client.gui.GuiResearchRecipe;

@Mixin(value = GuiResearchRecipe.class, remap = false)
public abstract class MixinVortexResearchPage {

    // Let TC draw the research heading and maintain its normal tooltip/page navigation first.
    @Inject(method = "drawPage", at = @At("RETURN"), require = 1)
    private void thaumicdabblery$vortex(ResearchPage entry, int side, int x, int y, int mx, int my, CallbackInfo ci) {
        if (entry instanceof VortexPage) VortexPageRenderer
            .draw((GuiResearchRecipe) (Object) this, (VortexPage) entry, x + side * 152 - 14, y - 8, mx, my);
    }
}
