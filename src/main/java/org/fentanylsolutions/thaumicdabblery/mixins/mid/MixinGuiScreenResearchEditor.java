package org.fentanylsolutions.thaumicdabblery.mixins.mid;

import net.minecraft.client.gui.GuiScreen;

import org.fentanylsolutions.thaumicdabblery.feature.researcheditor.BookEditorInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreenResearchEditor {

    @Inject(method = "handleInput", at = @At("TAIL"))
    private void thaumicdabblery$finishEditorInput(CallbackInfo ci) {
        if (BookEditorInput.handler != null) BookEditorInput.handler.afterInput((GuiScreen) (Object) this);
    }

    @Inject(method = "handleMouseInput", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$editorMouse(CallbackInfo ci) {
        if (BookEditorInput.handler != null && BookEditorInput.handler.mouse((GuiScreen) (Object) this)) ci.cancel();
    }

    @Inject(method = "handleKeyboardInput", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$editorKeyboard(CallbackInfo ci) {
        if (BookEditorInput.handler != null && BookEditorInput.handler.keyboard((GuiScreen) (Object) this)) ci.cancel();
    }
}
