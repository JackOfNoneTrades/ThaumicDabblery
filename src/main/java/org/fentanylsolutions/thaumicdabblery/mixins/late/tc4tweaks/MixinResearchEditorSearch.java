package org.fentanylsolutions.thaumicdabblery.mixins.late.tc4tweaks;

import net.glease.tc4tweak.modules.researchBrowser.ThaumonomiconIndexSearcher;
import net.minecraft.client.Minecraft;

import org.fentanylsolutions.thaumicdabblery.feature.researcheditor.BookEditorInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import thaumcraft.client.gui.GuiResearchBrowser;

@Mixin(value = ThaumonomiconIndexSearcher.class, remap = false)
public abstract class MixinResearchEditorSearch {

    // These handlers run outside the stock GUI, after its input and rendering hooks.
    // Keep the existing field and query so they work again when editing ends.
    @Inject(method = { "onGuiPreDraw", "onGuiPostDraw", "renderTick" }, at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$suspendSearchWhileEditing(CallbackInfo ci) {
        if (BookEditorInput.active() && Minecraft.getMinecraft().currentScreen instanceof GuiResearchBrowser)
            ci.cancel();
    }
}
