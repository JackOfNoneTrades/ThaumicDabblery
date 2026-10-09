package org.fentanylsolutions.thaumicdabblery.feature.osmotic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

public final class OsmoticPageButton extends GuiButton {

    private final int direction;

    public OsmoticPageButton(int id, int x, int y, int direction, String label) {
        super(id, x, y, direction == 0 ? 28 : 12, direction == 0 ? 9 : 12, label);
        this.direction = direction;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        if (direction == 0) {
            drawCenteredString(mc.fontRenderer, displayString, xPosition + width / 2, yPosition, 0xB0B0B0);
            return;
        }
        super.drawButton(mc, mouseX, mouseY);
        boolean hovered = mouseX >= xPosition && mouseY >= yPosition
            && mouseX < xPosition + width
            && mouseY < yPosition + height;
        int color = !enabled ? 0xFF707070 : hovered ? 0xFFFFFFA0 : 0xFFE0E0E0;
        for (int row = 0; row < 6; row++) {
            int offset = Math.min(row, 5 - row);
            int left = xPosition + 4 + (direction > 0 ? offset : 2 - offset);
            drawRect(left, yPosition + 3 + row, left + 2, yPosition + 4 + row, color);
        }
    }
}
