package org.fentanylsolutions.thaumicdabblery.feature.researcheditor;

import net.minecraft.client.gui.GuiScreen;

/** Optional client bridge: loading the vanilla input mixin must not load MineTweaker or Thaumcraft classes. */
public final class BookEditorInput {

    public interface Handler {

        boolean mouse(GuiScreen screen);

        boolean keyboard(GuiScreen screen);

        boolean active();

        void afterInput(GuiScreen screen);
    }

    public static Handler handler;

    private BookEditorInput() {}

    public static boolean active() {
        return handler != null && handler.active();
    }
}
