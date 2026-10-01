package org.fentanylsolutions.thaumicdabblery.mixins.late.tc4tweaks;

import net.glease.tc4tweak.modules.researchBrowser.DrawResearchCompletionCounter;

import org.fentanylsolutions.thaumicdabblery.feature.researcheditor.BookEditorInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import thaumcraft.client.gui.GuiResearchBrowser;

@Mixin(value = DrawResearchCompletionCounter.class, remap = false)
public abstract class MixinResearchCompletionCounter {

    @Inject(method = "drawCompletionCounter", at = @At("HEAD"), cancellable = true)
    private static void thaumicdabblery$reserveEditorHeader(GuiResearchBrowser browser, int x, int y, int mouseX,
        int mouseY, CallbackInfo ci) {
        if (BookEditorInput.active()) ci.cancel();
    }
}
