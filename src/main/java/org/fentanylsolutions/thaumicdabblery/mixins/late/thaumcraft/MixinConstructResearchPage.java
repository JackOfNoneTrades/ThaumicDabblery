package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import org.fentanylsolutions.thaumicdabblery.feature.construct.ConstructPage;
import org.fentanylsolutions.thaumicdabblery.feature.construct.ConstructPageRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import thaumcraft.api.research.ResearchPage;
import thaumcraft.client.gui.GuiResearchRecipe;

@Mixin(value = GuiResearchRecipe.class, remap = false)
public abstract class MixinConstructResearchPage {

    // Let TC draw the research heading and maintain its normal tooltip/page navigation first.
    @Inject(method = "drawPage", at = @At("RETURN"), require = 1)
    private void thaumicdabblery$construct(ResearchPage entry, int side, int x, int y, int mx, int my,
        CallbackInfo ci) {
        if (entry instanceof ConstructPage) ConstructPageRenderer
            .draw((GuiResearchRecipe) (Object) this, (ConstructPage) entry, x + side * 152 - 14, y - 8, mx, my);
    }
}
