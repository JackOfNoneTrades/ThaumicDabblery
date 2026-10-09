package org.fentanylsolutions.thaumicdabblery.feature.osmotic;

import java.util.ArrayList;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import thaumcraft.api.aspects.Aspect;
import thaumic.tinkerer.client.gui.GuiEnchanting;
import thaumic.tinkerer.client.lib.LibResources;

public final class OsmoticStartButton extends GuiButton {

    private static final ResourceLocation TEXTURE = new ResourceLocation(LibResources.GUI_ENCHANTER);
    private final GuiEnchanting parent;

    public OsmoticStartButton(GuiEnchanting parent, int x, int y) {
        super(0, x, y, 15, 15, "");
        this.parent = parent;
        enabled = !parent.enchanter.enchantments.isEmpty() && !parent.enchanter.working
            && OsmoticRecipes.canAfford(parent.enchanter);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        float brightness = enabled || parent.enchanter.working ? 1.0F : 0.45F;
        GL11.glColor4f(brightness, brightness, brightness, 1.0F);
        mc.getTextureManager()
            .bindTexture(TEXTURE);
        drawTexturedModalRect(xPosition, yPosition, 176, parent.enchanter.working ? 39 : 24, 15, 15);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (mouseX >= xPosition && mouseX < xPosition + width && mouseY >= yPosition && mouseY < yPosition + height) {
            String key = parent.enchanter.working ? "thaumicdabblery.osmotic.working"
                : parent.enchanter.enchantments.isEmpty() ? "thaumicdabblery.osmotic.select"
                    : !OsmoticRecipes.validWand(parent.enchanter.getStackInSlot(1)) ? "thaumicdabblery.osmotic.wand"
                        : enabled ? "ttmisc.startEnchant" : "thaumicdabblery.osmotic.insufficient";
            parent.tooltip = new ArrayList<>();
            parent.tooltip.add(StatCollector.translateToLocal(key));
            if (key.equals("thaumicdabblery.osmotic.insufficient")) {
                for (Aspect aspect : parent.enchanter.totalAspects.getAspectsSorted()) {
                    long missing = OsmoticRecipes.missingVis(parent.enchanter, aspect);
                    if (missing == Long.MAX_VALUE) {
                        parent.tooltip.add(StatCollector.translateToLocal("thaumicdabblery.osmotic.invalidDiscount"));
                        break;
                    }
                    if (missing > 0) {
                        String amount = String.format(Locale.ROOT, "%d.%02d", missing / 100, missing % 100)
                            .replaceAll("\\.?0+$", "");
                        parent.tooltip.add(
                            StatCollector.translateToLocalFormatted(
                                "thaumicdabblery.osmotic.missing",
                                aspect.getName(),
                                amount));
                    }
                }
            }
        }
    }
}
