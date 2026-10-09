package org.fentanylsolutions.thaumicdabblery.feature.osmotic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import thaumic.tinkerer.client.core.helper.ClientHelper;

public final class OsmoticTooltips {

    private static final Map<GuiScreen, Tooltip> PENDING = new WeakHashMap<>();

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new OsmoticTooltips());
    }

    public static void capture(GuiScreen gui, int x, int y, List<String> lines) {
        PENDING.put(gui, new Tooltip(x, y, new ArrayList<>(lines)));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeScreen(GuiScreenEvent.DrawScreenEvent.Pre event) {
        PENDING.remove(event.gui);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        render(event.gui);
    }

    public static void render(GuiScreen gui) {
        Tooltip tooltip = PENDING.remove(gui);
        if (tooltip == null) return;
        // Draw after container overlays, preserving the state the native helper changes.
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            ClientHelper.renderTooltip(tooltip.x, tooltip.y, tooltip.lines);
        } finally {
            GL11.glPopAttrib();
        }
    }

    private static final class Tooltip {

        private final int x, y;
        private final List<String> lines;

        private Tooltip(int x, int y, List<String> lines) {
            this.x = x;
            this.y = y;
            this.lines = lines;
        }
    }
}
