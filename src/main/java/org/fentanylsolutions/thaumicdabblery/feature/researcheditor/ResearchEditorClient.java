package org.fentanylsolutions.thaumicdabblery.feature.researcheditor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;

public final class ResearchEditorClient implements BookEditorInput.Handler {

    private static final KeyBinding KEY = new KeyBinding(
        "key.thaumicdabblery.researchEditor",
        Keyboard.KEY_NONE,
        "key.categories.thaumicdabblery");
    private static volatile boolean enabled;
    private static ResearchEditorOverlay overlay;
    private static int tabPage;
    private static final Set<String> VISIBLE_KEYS = new java.util.HashSet<>();
    private static final ArrayList<String> VISIBLE_RESEARCH = new ArrayList<String>() {

        @Override
        public boolean contains(Object key) {
            return VISIBLE_KEYS.contains(key);
        }
    };

    public static void register() {
        ResearchEditorClient handler = new ResearchEditorClient();
        ClientRegistry.registerKeyBinding(KEY);
        FMLCommonHandler.instance()
            .bus()
            .register(handler);
        BookEditorInput.handler = handler;
        MinecraftForge.EVENT_BUS.register(handler);
    }

    public static boolean available() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.theWorld != null && mc.isSingleplayer()
            && mc.getIntegratedServer() != null
            && !mc.getIntegratedServer()
                .getPublic();
    }

    public static boolean enabled() {
        return enabled && available();
    }

    public static boolean toggle() {
        if (!available()) throw new IllegalArgumentException(
            "Thaumonomicon editing is only available in single-player, with LAN sharing off.");
        String problem = ResearchEditor.problem();
        if (!enabled && problem != null) throw new IllegalArgumentException(problem);
        enabled = !enabled;
        return enabled;
    }

    public static boolean isToggleKey(int key) {
        return key != Keyboard.KEY_NONE && key == KEY.getKeyCode();
    }

    public static boolean handleBookKey(int key) {
        if (!isToggleKey(key)) return false;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            toggle();
            if (mc.currentScreen instanceof GuiResearchBrowser) beginFrame((GuiResearchBrowser) mc.currentScreen);
            else if (enabled) mc.displayGuiScreen(new GuiResearchBrowser());
        } catch (IllegalArgumentException exception) {
            mc.thePlayer.addChatMessage(new ChatComponentText(exception.getMessage()));
        }
        return true;
    }

    public static String selectedTab() {
        return ReflectionHelper.getPrivateValue(GuiResearchBrowser.class, null, "selectedCategory");
    }

    public static void selectTab(GuiResearchBrowser browser, String tab) {
        ReflectionHelper.setPrivateValue(GuiResearchBrowser.class, null, tab, "selectedCategory");
        browser.updateResearch();
    }

    public static void done(String tab) {
        enabled = false;
        if (overlay != null) {
            overlay.detach();
            overlay = null;
        }
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen instanceof GuiResearchBrowser) ((GuiResearchBrowser) screen).updateResearch();
    }

    public static void beginFrame(GuiResearchBrowser browser) {
        if (!enabled()) {
            if (overlay != null) done(selectedTab());
            return;
        }
        if (ResearchEditor.problem() != null) {
            done(selectedTab());
            return;
        }
        if (overlay == null || overlay.browser != browser) {
            if (overlay != null) overlay.detach();
            tabPage = Math
                .max(0, new ArrayList<>(ResearchCategories.researchCategories.keySet()).indexOf(selectedTab()))
                / (tabsPerSide() * 2);
            overlay = new ResearchEditorOverlay(browser);
        }
        ReflectionHelper.setPrivateValue(GuiResearchBrowser.class, browser, null, "currentHighlight");
        overlay.prepare();
    }

    public static void draw(GuiResearchBrowser browser, int x, int y, float partial) {
        if (enabled() && overlay != null && overlay.browser == browser) overlay.drawScreen(x, y, partial);
    }

    public static ArrayList<String> visibleResearch() {
        return VISIBLE_RESEARCH;
    }

    public static void refreshVisibility() {
        VISIBLE_RESEARCH.clear();
        VISIBLE_KEYS.clear();
        for (ResearchCategoryList category : ResearchCategories.researchCategories.values()) {
            for (String key : category.research.keySet()) {
                VISIBLE_RESEARCH.add(key);
                VISIBLE_RESEARCH.add("@" + key);
                VISIBLE_KEYS.add(key);
                VISIBLE_KEYS.add("@" + key);
            }
        }
    }

    public static int tabsPerSide() {
        return Loader.isModLoaded("tc4tweak") ? Tweaks.tabsPerSide() : 9;
    }

    public static Set<String> visibleTabs() {
        ArrayList<String> tabs = new ArrayList<>(ResearchCategories.researchCategories.keySet());
        int size = tabsPerSide() * 2;
        tabPage = Math.max(0, Math.min(tabPage, Math.max(0, (tabs.size() - 1) / size)));
        return new LinkedHashSet<>(tabs.subList(tabPage * size, Math.min(tabs.size(), (tabPage + 1) * size)));
    }

    public static void turnPage(int direction) {
        tabPage += direction;
        visibleTabs();
    }

    @Override
    public boolean active() {
        return enabled();
    }

    @Override
    public boolean mouse(GuiScreen screen) {
        if ((screen instanceof GuiResearchBrowser || screen instanceof GuiResearchRecipe) && Mouse.getEventButtonState()
            && isToggleKey(Mouse.getEventButton() - 100)) return handleBookKey(Mouse.getEventButton() - 100);
        if (!(screen instanceof GuiResearchBrowser) || !enabled()) return false;
        beginFrame((GuiResearchBrowser) screen);
        if (overlay == null) return false;
        int x = Mouse.getEventX() * screen.width / Minecraft.getMinecraft().displayWidth;
        int y = screen.height - Mouse.getEventY() * screen.height / Minecraft.getMinecraft().displayHeight - 1;
        int button = Mouse.getEventButton();
        if (Mouse.getEventButtonState()) overlay.mouseClicked(x, y, button);
        else if (button >= 0) overlay.mouseMovedOrUp(x, y, button);
        else overlay.mouseClickMove(x, y, 0, 0);
        // Done may detach the overlay during this event.
        if (overlay != null && Mouse.getEventDWheel() != 0) overlay.scroll(x, Mouse.getEventDWheel() > 0 ? -1 : 1);
        return true;
    }

    @Override
    public void afterInput(GuiScreen screen) {
        if (enabled() && screen instanceof GuiResearchBrowser) {
            // The editor consumed wheel events; do not let Salis' post-input shortcut switch tabs again.
            Mouse.getDWheel();
            ReflectionHelper
                .setPrivateValue(GuiResearchBrowser.class, (GuiResearchBrowser) screen, null, "currentHighlight");
        }
    }

    @Override
    public boolean keyboard(GuiScreen screen) {
        if ((screen instanceof GuiResearchBrowser || screen instanceof GuiResearchRecipe) && Keyboard.getEventKeyState()
            && isToggleKey(Keyboard.getEventKey())) return handleBookKey(Keyboard.getEventKey());
        if (!(screen instanceof GuiResearchBrowser) || !enabled()) return false;
        beginFrame((GuiResearchBrowser) screen);
        if (overlay == null) return false;
        if (Keyboard.getEventKeyState()) overlay.keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());
        return true;
    }

    @SubscribeEvent
    public void disconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        enabled = false;
        overlay = null;
        ResearchEditor.forgetHistory();
    }

    @SubscribeEvent
    public void beforeBookInit(GuiScreenEvent.InitGuiEvent.Pre event) {
        if (enabled() && event.gui instanceof GuiResearchBrowser && Loader.isModLoaded("salisarcana"))
            PageMemory.suspend();
    }

    @SubscribeEvent
    public void afterBookInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiResearchBrowser && Loader.isModLoaded("salisarcana")) PageMemory.restore();
    }

    /** Keep Salis' saved page intact, but let an armed editor open the research map. */
    private static final class PageMemory {

        private static final java.util.Stack<net.minecraft.util.Tuple> SAVED = new java.util.Stack<>();

        private static void suspend() {
            SAVED.addAll(dev.rndmorris.salisarcana.lib.ThaumonomiconGuiHelper.RightClickClose$ScreenStack);
            dev.rndmorris.salisarcana.lib.ThaumonomiconGuiHelper.RightClickClose$ScreenStack.clear();
        }

        private static void restore() {
            if (SAVED.isEmpty()) return;
            dev.rndmorris.salisarcana.lib.ThaumonomiconGuiHelper.RightClickClose$ScreenStack.addAll(SAVED);
            SAVED.clear();
        }
    }

    private static final class Tweaks {

        private static int tabsPerSide() {
            return net.glease.tc4tweak.modules.researchBrowser.BrowserPaging.getTabPerSide();
        }
    }
}
