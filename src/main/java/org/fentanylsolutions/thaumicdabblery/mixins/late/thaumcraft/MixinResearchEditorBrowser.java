package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import java.util.HashMap;

import org.fentanylsolutions.thaumicdabblery.feature.researcheditor.ResearchBrowserAccess;
import org.fentanylsolutions.thaumicdabblery.feature.researcheditor.ResearchEditorClient;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import thaumcraft.api.research.ResearchItem;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.common.lib.research.ResearchManager;

@Mixin(value = GuiResearchBrowser.class, remap = false)
public abstract class MixinResearchEditorBrowser implements ResearchBrowserAccess {

    @Shadow
    protected int paneWidth;
    @Shadow
    protected int paneHeight;
    @Shadow
    protected double guiMapX;
    @Shadow
    protected double guiMapY;
    @Shadow
    protected double field_74117_m;
    @Shadow
    protected double field_74115_n;
    @Shadow
    protected double field_74124_q;
    @Shadow
    protected double field_74123_r;
    @Shadow
    private static int guiMapTop;
    @Shadow
    private static int guiMapLeft;
    @Shadow
    private static int guiMapBottom;
    @Shadow
    private static int guiMapRight;
    @Shadow
    private ResearchItem currentHighlight;

    @Override
    public int thaumicdabblery$width() {
        return paneWidth;
    }

    @Override
    public int thaumicdabblery$height() {
        return paneHeight;
    }

    @Override
    public double thaumicdabblery$mapX() {
        return guiMapX;
    }

    @Override
    public double thaumicdabblery$mapY() {
        return guiMapY;
    }

    @Override
    public void thaumicdabblery$pan(double x, double y) {
        guiMapX = field_74117_m = field_74124_q = x;
        guiMapY = field_74115_n = field_74123_r = y;
    }

    @Inject(method = { "drawScreen", "func_73863_a" }, at = @At("HEAD"))
    private void thaumicdabblery$beginEditor(int x, int y, float partial, CallbackInfo ci) {
        ResearchEditorClient.beginFrame((GuiResearchBrowser) (Object) this);
        if (ResearchEditorClient.enabled()) {
            // Leave room to drag beyond the old research bounds. updateResearch restores native bounds on exit.
            guiMapTop = guiMapLeft = -240000;
            guiMapBottom = guiMapRight = 240000;
            currentHighlight = null;
        }
    }

    @Inject(method = { "drawScreen", "func_73863_a" }, at = @At("TAIL"))
    private void thaumicdabblery$drawEditor(int x, int y, float partial, CallbackInfo ci) {
        ResearchEditorClient.draw((GuiResearchBrowser) (Object) this, x, y, partial);
    }

    @Redirect(
        method = { "drawScreen", "func_73863_a" },
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;isButtonDown(I)Z"))
    private boolean thaumicdabblery$preventNativeDrag(int button) {
        return !ResearchEditorClient.enabled() && Mouse.isButtonDown(button);
    }

    @Redirect(
        method = "genResearchBackground",
        at = @At(value = "INVOKE", target = "Ljava/util/HashMap;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object thaumicdabblery$editorVisibility(HashMap<?, ?> map, Object key) {
        return ResearchEditorClient.enabled() && map == GuiResearchBrowser.completedResearch
            ? ResearchEditorClient.visibleResearch()
            : map.get(key);
    }

    @Redirect(
        method = { "drawScreen", "func_73863_a", "genResearchBackground" },
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/common/lib/research/ResearchManager;isResearchComplete(Ljava/lang/String;Ljava/lang/String;)Z"))
    private boolean thaumicdabblery$showEldritch(String player, String key) {
        return ResearchEditorClient.enabled() || ResearchManager.isResearchComplete(player, key);
    }

    @Inject(method = "canUnlockResearch", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$showResearch(ResearchItem research, CallbackInfoReturnable<Boolean> cir) {
        if (ResearchEditorClient.enabled()) cir.setReturnValue(true);
    }

    @Redirect(
        method = "genResearchBackground",
        at = @At(value = "INVOKE", target = "Lthaumcraft/api/research/ResearchItem;isVirtual()Z"))
    private boolean thaumicdabblery$showVirtual(ResearchItem research) {
        return !ResearchEditorClient.enabled() && research.isVirtual();
    }

    @ModifyVariable(method = "genResearchBackground", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int thaumicdabblery$editorTooltip(int mouseX) {
        // No native hover means no purchase tooltip or Salis Arcana creative-completion shortcut.
        return ResearchEditorClient.enabled() ? Integer.MIN_VALUE : mouseX;
    }

    @Inject(method = { "doesGuiPauseGame", "func_73868_f" }, at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$pauseWhileEditing(CallbackInfoReturnable<Boolean> cir) {
        if (ResearchEditorClient.enabled()) cir.setReturnValue(true);
    }
}
