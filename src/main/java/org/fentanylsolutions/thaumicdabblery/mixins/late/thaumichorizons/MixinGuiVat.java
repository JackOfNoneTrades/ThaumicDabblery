package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumichorizons;

import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.witcherybranch.WitcheryBranchFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.emoniph.witchery.Witchery;
import com.kentington.thaumichorizons.client.gui.GuiVat;
import com.kentington.thaumichorizons.common.tiles.TileVat;

@Mixin(value = GuiVat.class, remap = false)
public abstract class MixinGuiVat {

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 209;

    @Shadow
    private TileVat tile;

    @Inject(method = "drawGuiContainerBackgroundLayer", at = @At("TAIL"))
    private void thaumicdabblery$drawMysticBranchInfusion(float partialTicks, int mouseX, int mouseY, CallbackInfo ci) {
        if (!WitcheryBranchFeature.isActive() || tile.selfInfusions == null) {
            return;
        }

        GuiVat gui = (GuiVat) (Object) this;
        int left = (gui.width - GUI_WIDTH) / 2;
        int top = (gui.height - GUI_HEIGHT) / 2;
        for (int slot = 0; slot < tile.selfInfusions.length; slot++) {
            if (tile.selfInfusions[slot] == 0) {
                break;
            }
            if (tile.selfInfusions[slot] == WitcheryBranchFeature.INFUSION_ID) {
                RenderItem.getInstance()
                    .renderItemAndEffectIntoGUI(
                        gui.mc.fontRenderer,
                        gui.mc.getTextureManager(),
                        new ItemStack(Witchery.Items.MYSTIC_BRANCH),
                        left + 55 + 16 * (slot % 4),
                        top + 56 + 17 * (slot / 4));
            }
        }
    }
}
