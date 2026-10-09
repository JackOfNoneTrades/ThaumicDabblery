package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumictinkerer;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.osmotic.OsmoticPageButton;
import org.fentanylsolutions.thaumicdabblery.feature.osmotic.OsmoticRecipes;
import org.fentanylsolutions.thaumicdabblery.feature.osmotic.OsmoticStartButton;
import org.fentanylsolutions.thaumicdabblery.feature.osmotic.OsmoticTooltips;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import thaumic.tinkerer.client.gui.GuiEnchanting;
import thaumic.tinkerer.client.gui.button.GuiButtonEnchantment;
import thaumic.tinkerer.common.block.tile.TileEnchanter;

@Mixin(value = GuiEnchanting.class, remap = false)
public abstract class MixinGuiEnchanting extends GuiContainer {

    @Shadow
    public TileEnchanter enchanter;
    @Shadow
    int x, y;
    @Shadow
    ItemStack currentStack;
    @Shadow
    GuiButtonEnchantment[] enchantButtons;

    @Shadow
    public abstract void buildButtonList();

    @Unique
    private int thaumicdabblery$page, thaumicdabblery$pages = 1;
    @Unique
    private ItemStack thaumicdabblery$stack;

    protected MixinGuiEnchanting(Container container) {
        super(container);
    }

    @Redirect(
        method = "drawGuiContainerForegroundLayer",
        at = @At(
            value = "INVOKE",
            target = "Lthaumic/tinkerer/client/core/helper/ClientHelper;renderTooltip(IILjava/util/List;)V",
            remap = false),
        remap = true,
        require = 1)
    private void thaumicdabblery$tooltipState(int mouseX, int mouseY, List<String> lines) {
        OsmoticTooltips.capture(this, mouseX + x, mouseY + y, lines);
    }

    @Inject(method = "buildButtonList", at = @At("HEAD"), require = 1)
    private void thaumicdabblery$clean(CallbackInfo ci) {
        OsmoticRecipes.refresh(enchanter);
    }

    @Inject(method = "asignEnchantButtons", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$page(CallbackInfo ci) {
        if (!ItemStack.areItemStacksEqual(currentStack, thaumicdabblery$stack)) {
            thaumicdabblery$page = 0;
            thaumicdabblery$stack = currentStack == null ? null : currentStack.copy();
        }
        List<Integer> available = OsmoticRecipes.available(currentStack, mc.thePlayer, enchanter.enchantments);
        thaumicdabblery$pages = Math.max(1, (available.size() + 15) / 16);
        thaumicdabblery$page = Math.min(thaumicdabblery$page, thaumicdabblery$pages - 1);
        int start = thaumicdabblery$page * 16, count = Math.min(16, available.size() - start);
        for (int i = 0; i < 16; i++) {
            GuiButtonEnchantment button = enchantButtons[i];
            button.enabled = i < count;
            button.enchant = i < count ? Enchantment.enchantmentsList[available.get(start + i)] : null;
            button.yPosition = y + (count > 8 && i < 8 ? 30 : 54);
        }
        ci.cancel();
    }

    @Inject(method = "buildButtonList", at = @At("TAIL"), require = 1)
    private void thaumicdabblery$buttons(CallbackInfo ci) {
        // Keep the start/progress control visible beside the vis bars, even before selection.
        for (int i = 0; i < buttonList.size(); i++) {
            if (((GuiButton) buttonList.get(i)).id == 0) {
                buttonList.set(
                    i,
                    new OsmoticStartButton(
                        (GuiEnchanting) (Object) this,
                        x + 151,
                        y + (enchantButtons[8].enabled ? 9 : 33)));
                break;
            }
        }
        if (thaumicdabblery$pages <= 1) return;
        GuiButton previous = new OsmoticPageButton(-31000, x + 1, y + 52, -1, "");
        GuiButton label = new OsmoticPageButton(
            -30998,
            x,
            y + 66,
            0,
            (thaumicdabblery$page + 1) + "/" + thaumicdabblery$pages);
        GuiButton next = new OsmoticPageButton(-30999, x + 15, y + 52, 1, "");
        previous.enabled = thaumicdabblery$page > 0;
        next.enabled = thaumicdabblery$page + 1 < thaumicdabblery$pages;
        label.enabled = false;
        buttonList.add(previous);
        buttonList.add(label);
        buttonList.add(next);
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), remap = true, cancellable = true, require = 1)
    private void thaumicdabblery$turn(GuiButton button, CallbackInfo ci) {
        if (button.id >= -31000 && button.id <= -30998) {
            if (button.enabled && button.id == -31000) thaumicdabblery$page = Math.max(0, thaumicdabblery$page - 1);
            if (button.enabled && button.id == -30999)
                thaumicdabblery$page = Math.min(thaumicdabblery$pages - 1, thaumicdabblery$page + 1);
            buildButtonList();
            ci.cancel();
        }
    }
}
