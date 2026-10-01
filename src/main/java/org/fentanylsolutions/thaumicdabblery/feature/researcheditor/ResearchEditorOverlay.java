package org.fentanylsolutions.thaumicdabblery.feature.researcheditor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.ReflectionHelper;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.client.gui.GuiResearchBrowser;

/** Drawing and input layer attached to a stock GuiResearchBrowser. Never opened as a screen. */
public final class ResearchEditorOverlay extends GuiScreen {

    private static final int CELL = 24, ROW = 16;
    public final GuiResearchBrowser browser;
    private final ResearchBrowserAccess geometry;
    private final Map<GuiButton, Boolean> nativeButtons = new IdentityHashMap<>();
    private int left, top, right, bottom, toolbarY, categoryCount;

    private enum Pick {
        NONE,
        TAB,
        PARENT,
        SWAP
    }

    private ResearchLayout layout;
    private String tab, selected, source, previousParent;
    private Pick pick = Pick.NONE;
    private boolean hiddenLink;
    private int generation, lastMouseX, lastMouseY, dragX, dragY;
    private boolean panning, dragging, moved;
    private String status = "Saved", notice = "Drag research to move it. Right-click for more actions.";
    private boolean saveFailed;
    private long noticeUntil;
    private final List<MenuItem> menu = new ArrayList<>();
    private String menuTitle, menuKey;
    private int menuX, menuY, menuWidth, menuScroll;
    private GuiButton undo, redo;

    public ResearchEditorOverlay(GuiResearchBrowser browser) {
        this.browser = browser;
        geometry = (ResearchBrowserAccess) browser;
        tab = ResearchEditorClient.selectedTab();
        mc = Minecraft.getMinecraft();
        fontRendererObj = mc.fontRenderer;
        refresh();
    }

    public void prepare() {
        int oldLeft = left, oldTop = top, oldRight = right, oldBottom = bottom;
        width = browser.width;
        height = browser.height;
        left = (width - geometry.thaumicdabblery$width()) / 2 + 16;
        top = (height - geometry.thaumicdabblery$height()) / 2 + 17;
        right = left + geometry.thaumicdabblery$width() - 32;
        bottom = top + geometry.thaumicdabblery$height() - 34;
        toolbarY = bottom + 2;
        if (left != oldLeft || top != oldTop
            || right != oldRight
            || bottom != oldBottom
            || buttonList.isEmpty()
            || categoryCount != ResearchCategories.researchCategories.size()) initGui();
        geometry.thaumicdabblery$pan(geometry.thaumicdabblery$mapX(), geometry.thaumicdabblery$mapY());
        List<GuiButton> buttons = ReflectionHelper
            .getPrivateValue(GuiScreen.class, browser, "buttonList", "field_146292_n");
        for (GuiButton button : buttons) {
            if (!nativeButtons.containsKey(button)) nativeButtons.put(button, button.visible);
            button.visible = false;
        }
        tab = ResearchEditorClient.selectedTab();
        updateScreen();
    }

    public void detach() {
        for (Map.Entry<GuiButton, Boolean> button : nativeButtons.entrySet())
            button.getKey().visible = button.getValue();
        nativeButtons.clear();
    }

    private void refresh() {
        layout = ResearchEditor.layout();
        generation = ResearchEditor.generation();
        if (!ResearchCategories.researchCategories.containsKey(tab))
            tab = ResearchCategories.researchCategories.keySet()
                .iterator()
                .next();
        ResearchEditorClient.refreshVisibility();
        ResearchEditorClient.selectTab(browser, tab);
        if (selected != null && (layout.entries.get(selected) == null || layout.entries.get(selected).deleted))
            selected = null;
    }

    @Override
    public void initGui() {
        categoryCount = ResearchCategories.researchCategories.size();
        buttonList.clear();
        undo = new GuiButton(0, left + 23, toolbarY, 44, 13, "Undo");
        redo = new GuiButton(1, left + 70, toolbarY, 44, 13, "Redo");
        buttonList.add(undo);
        buttonList.add(redo);
        buttonList.add(new GuiButton(2, right - 67, toolbarY, 44, 13, "Done"));
        if (ResearchCategories.researchCategories.size() > ResearchEditorClient.tabsPerSide() * 2) {
            buttonList.add(new GuiButton(3, left, toolbarY, 18, 13, "<"));
            buttonList.add(new GuiButton(4, right - 18, toolbarY, 18, 13, ">"));
        }
    }

    @Override
    public void updateScreen() {
        if (!ResearchEditorClient.enabled()) {
            ResearchEditorClient.done(tab);
            return;
        }
        if (generation != ResearchEditor.generation()) {
            cancel();
            if (ResearchEditor.problem() != null) {
                ResearchEditorClient.done(tab);
                return;
            }
            refresh();
            notice = "Scripts reloaded. Undo history cleared.";
            status = "Saved";
            saveFailed = false;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id >= 3) {
            ResearchEditorClient.turnPage(button.id == 3 ? -1 : 1);
            return;
        }
        cancel();
        if (button.id == 2) ResearchEditorClient.done(tab);
        else history(button.id == 0);
    }

    private void history(boolean backwards) {
        try {
            String name = backwards ? ResearchEditor.undoName() : ResearchEditor.redoName();
            if (name == null) return;
            if (backwards) ResearchEditor.undo();
            else ResearchEditor.redo();
            refresh();
            saved((backwards ? "Undid: " : "Redid: ") + name);
        } catch (IOException exception) {
            failure(exception);
        }
    }

    private void edit(String name, Consumer<ResearchLayout> action) {
        try {
            if (ResearchEditor.edit(name, action)) {
                refresh();
                saved(name);
            } else saved("No change");
        } catch (IOException | IllegalArgumentException exception) {
            failure(exception);
        }
    }

    private void saved(String message) {
        status = "Saved";
        saveFailed = false;
        notice = message;
        noticeUntil = System.currentTimeMillis() + 4000;
    }

    private void failure(Exception exception) {
        status = exception instanceof IOException ? "Save failed" : "Not applied";
        saveFailed = true;
        notice = exception.getMessage();
    }

    @Override
    public void drawScreen(int mx, int my, float partial) {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1, 1, 1, 1);
        scissor(true);
        for (Map.Entry<String, ResearchLayout.Entry> pair : layout.entries.entrySet()) {
            ResearchLayout.Entry entry = pair.getValue();
            if (entry.deleted || !entry.tab.equals(tab)) continue;
            drawLinks(pair.getKey(), entry, entry.hidden, true);
            if (dragging) drawLinks(pair.getKey(), entry, entry.parents, false);
            int x = screenX(entry.x), y = screenY(entry.y);
            if (pair.getKey()
                .equals(selected)
                || pair.getKey()
                    .equals(source))
                border(x - 13, y - 13, 26, 26, 0xff63d9e0);
            if (entry.hasFlag(0) || entry.hasFlag(1) || entry.research.isVirtual()) {
                String badge = (entry.hasFlag(0) ? "L" : "") + (entry.hasFlag(1) ? "H" : "")
                    + (entry.research.isVirtual() ? "V" : "");
                fontRendererObj.drawStringWithShadow(badge, x - 11, y + 5, 0xffffe8a4);
            }
        }
        if (dragging && selected != null) {
            int x = screenX(dragX), y = screenY(dragY);
            boolean blocked = layout.occupied(selected, tab, dragX, dragY);
            drawRect(x - 12, y - 12, x + 12, y + 12, blocked ? 0x99bc3843 : 0x9944b8a0);
            border(x - 13, y - 13, 26, 26, blocked ? 0xffff5963 : 0xff63d9e0);
        }
        scissor(false);
        if (pick == Pick.TAB) drawRect(left - 16, top - 17, right + 16, bottom + 17, 0xb5181d26);
        drawRect(left - 1, top - 15, right + 1, top - 2, 0xe6181d26);
        drawString(fontRendererObj, "EDIT MODE", left + 3, top - 12, 0xffdfc38c);
        drawString(
            fontRendererObj,
            status,
            right - fontRendererObj.getStringWidth(status) - 3,
            top - 12,
            saveFailed ? 0xffff8d8d : 0xff9bd4b5);
        undo.enabled = ResearchEditor.undoName() != null;
        redo.enabled = ResearchEditor.redoName() != null;
        super.drawScreen(mx, my, partial);
        String instruction = pick == Pick.TAB ? "Select tab to move " + name(source) + " to"
            : pick == Pick.PARENT ? "Select a parent for " + name(source)
                : pick == Pick.SWAP ? "Select research to swap with " + name(source) : null;
        if (instruction != null) {
            drawRect(left + 2, top + 2, right - 2, top + 25, 0xe6181d26);
            fontRendererObj.drawSplitString(instruction, left + 5, top + 5, right - left - 10, 0xfff2e1b9);
        }
        if (dragging) {
            String position = dragX + ", "
                + dragY
                + (layout.occupied(selected, tab, dragX, dragY) ? " (occupied)" : "");
            drawString(fontRendererObj, position, left + 3, bottom - 12, 0xffffe8a4);
        } else if (saveFailed || System.currentTimeMillis() < noticeUntil) {
            drawRect(left + 2, bottom - 24, right - 2, bottom - 1, 0xe6181d26);
            List<String> lines = fontRendererObj.listFormattedStringToWidth(notice, right - left - 10);
            for (int i = 0; i < Math.min(2, lines.size()); i++) drawString(
                fontRendererObj,
                lines.get(i),
                left + 5,
                bottom - 22 + i * 10,
                saveFailed ? 0xffff9393 : 0xffd7e2d7);
        }
        if (!menu.isEmpty()) drawMenu(mx, my);
        else if (!dragging && !panning) {
            String target = hit(mx, my);
            if (target != null) {
                ResearchLayout.Entry entry = layout.require(target);
                List<String> tooltip = new ArrayList<>(
                    Arrays.asList(name(target), target, entry.tab + " (" + entry.x + ", " + entry.y + ")"));
                for (int i = 0; i < ResearchLayout.FLAGS.length; i++)
                    if (entry.hasFlag(i)) tooltip.add(ResearchLayout.FLAGS[i]);
                if (entry.research.isVirtual()) tooltip.add("Virtual research");
                if (pick == Pick.PARENT) {
                    try {
                        layout.copy()
                            .parent(source, previousParent, target, hiddenLink);
                    } catch (IllegalArgumentException exception) {
                        tooltip.add("\u00a7c" + exception.getMessage());
                    }
                }
                drawHoveringText(tooltip, mx, my, fontRendererObj);
            } else if (mx >= undo.xPosition && mx <= redo.xPosition + redo.width
                && my >= toolbarY
                && my <= toolbarY + 13) {
                    String label = mx < redo.xPosition ? ResearchEditor.undoName() : ResearchEditor.redoName();
                    if (label != null) drawHoveringText(Arrays.asList(label), mx, my, fontRendererObj);
                } else if (mx >= left && mx < right && my >= top - 15 && my < top - 2) {
                    drawHoveringText(
                        fontRendererObj.listFormattedStringToWidth(notice, Math.min(280, width - 24)),
                        mx,
                        my,
                        fontRendererObj);
                }
        }
        GL11.glColor4f(1, 1, 1, 1);
    }

    private void drawLinks(String key, ResearchLayout.Entry child, String[] parents, boolean hidden) {
        if (parents == null) return;
        int x = screenX(dragging && key.equals(selected) ? dragX : child.x);
        int y = screenY(dragging && key.equals(selected) ? dragY : child.y);
        for (String parent : parents) {
            ResearchLayout.Entry entry = layout.entries.get(parent);
            if (entry == null || entry.deleted || !entry.tab.equals(tab)) continue;
            int px = screenX(dragging && parent.equals(selected) ? dragX : entry.x);
            int py = screenY(dragging && parent.equals(selected) ? dragY : entry.y);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            if (hidden) {
                GL11.glEnable(GL11.GL_LINE_STIPPLE);
                GL11.glLineStipple(1, (short) 0x00ff);
                GL11.glColor4f(.43f, .4f, .57f, 1);
            } else GL11.glColor4f(.42f, .33f, .20f, 1);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2i(px, py);
            GL11.glVertex2i(x, y);
            GL11.glEnd();
            GL11.glDisable(GL11.GL_LINE_STIPPLE);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
    }

    private void scissor(boolean enable) {
        if (!enable) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            return;
        }
        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int scale = resolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(left * scale, (height - bottom) * scale, (right - left) * scale, (bottom - top) * scale);
    }

    private static void border(int x, int y, int w, int h, int color) {
        drawRect(x, y, x + w, y + 1, color);
        drawRect(x, y + h - 1, x + w, y + h, color);
        drawRect(x, y, x + 1, y + h, color);
        drawRect(x + w - 1, y, x + w, y + h, color);
    }

    private int screenX(int x) {
        return left + 11 + x * CELL - (int) Math.floor(geometry.thaumicdabblery$mapX());
    }

    private int screenY(int y) {
        return top + 11 + y * CELL - (int) Math.floor(geometry.thaumicdabblery$mapY());
    }

    private int gridX(int x) {
        return Math.round((x - screenX(0)) / (float) CELL);
    }

    private int gridY(int y) {
        return Math.round((y - screenY(0)) / (float) CELL);
    }

    private boolean onMap(int x, int y) {
        return x >= left && x < right && y >= top && y < bottom;
    }

    private String hit(int x, int y) {
        if (!onMap(x, y) || pick == Pick.TAB) return null;
        String fallback = null;
        for (Map.Entry<String, ResearchLayout.Entry> pair : layout.entries.entrySet()) {
            ResearchLayout.Entry entry = pair.getValue();
            if (!entry.deleted && entry.tab.equals(tab)
                && Math.abs(x - screenX(entry.x)) <= 11
                && Math.abs(y - screenY(entry.y)) <= 11) {
                if (pair.getKey()
                    .equals(selected)) return selected;
                if (fallback == null || !entry.research.isVirtual()) fallback = pair.getKey();
            }
        }
        return fallback;
    }

    private String name(String key) {
        ResearchLayout.Entry entry = layout.entries.get(key);
        return entry == null ? String.valueOf(key) : entry.research.getName();
    }

    private String fit(String text, int pixels) {
        return fontRendererObj.trimStringToWidth(text, Math.max(8, pixels));
    }

    private void focus(String key) {
        ResearchLayout.Entry entry = layout.entries.get(key);
        if (entry == null) {
            for (ResearchLayout.Entry candidate : layout.entries.values())
                if (!candidate.deleted && candidate.tab.equals(tab)) {
                    entry = candidate;
                    break;
                }
        }
        if (entry != null) geometry
            .thaumicdabblery$pan(entry.x * CELL + 11 - (right - left) / 2, entry.y * CELL + 11 - (bottom - top) / 2);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        if (ResearchEditorClient.isToggleKey(button - 100)) {
            ResearchEditorClient.done(tab);
            return;
        }
        if (!menu.isEmpty()) {
            if (x >= menuX && x <= menuX + menuWidth && y >= menuY && y < menuY + 30 + menuRows() * ROW) {
                if (button != 0) return;
                int index = (y - menuY - 30) / ROW + menuScroll;
                if (y >= menuY + 30 && index >= 0 && index < menu.size()) {
                    Runnable action = menu.get(index).action;
                    menu.clear();
                    action.run();
                }
                return;
            }
            menu.clear();
            return;
        }
        if (button == 0 && y >= toolbarY && y < toolbarY + 13) {
            undo.enabled = ResearchEditor.undoName() != null;
            redo.enabled = ResearchEditor.redoName() != null;
            super.mouseClicked(x, y, button);
            return;
        }
        String targetTab = tabAt(x, y);
        if (button == 0 && targetTab != null) {
            if (pick == Pick.TAB) {
                String moving = source;
                edit("Moved " + name(moving) + " to " + targetTab, next -> next.moveToTab(moving, targetTab));
                if (!saveFailed) {
                    tab = targetTab;
                    ResearchEditorClient.selectTab(browser, tab);
                    selected = moving;
                    focus(moving);
                    cancel();
                }
            } else {
                tab = targetTab;
                ResearchEditorClient.selectTab(browser, tab);
                focus(null);
            }
            return;
        }
        String target = hit(x, y);
        if (pick != Pick.NONE) {
            if (button == 0 && target != null) {
                if (pick == Pick.PARENT) {
                    String child = source, old = previousParent;
                    boolean hidden = hiddenLink;
                    edit("Changed parents of " + name(child), next -> next.parent(child, old, target, hidden));
                } else if (pick == Pick.SWAP) {
                    String first = source;
                    edit("Swapped " + name(first) + " and " + name(target), next -> next.swap(first, target));
                }
                if (!saveFailed) cancel();
            }
            return;
        }
        if (button == 1 && target != null) {
            selected = target;
            rootMenu(target, x, y);
        } else if (button == 0 && onMap(x, y)) {
            selected = target;
            lastMouseX = x;
            lastMouseY = y;
            panning = target == null;
            dragging = target != null;
            moved = false;
            if (dragging) {
                dragX = layout.require(target).x;
                dragY = layout.require(target).y;
            }
        }
    }

    @Override
    protected void mouseClickMove(int x, int y, int button, long elapsed) {
        if (button != 0) return;
        if (panning) {
            geometry.thaumicdabblery$pan(
                geometry.thaumicdabblery$mapX() - x + lastMouseX,
                geometry.thaumicdabblery$mapY() - y + lastMouseY);
        }
        if (dragging && (moved || Math.abs(x - lastMouseX) + Math.abs(y - lastMouseY) > 3)) {
            moved = true;
            dragX = gridX(x);
            dragY = gridY(y);
        }
        if (panning) {
            lastMouseX = x;
            lastMouseY = y;
        }
    }

    @Override
    protected void mouseMovedOrUp(int x, int y, int button) {
        if (button == 0) {
            if (dragging && moved && onMap(x, y)) {
                String key = selected;
                int column = dragX, row = dragY;
                edit("Moved " + name(key), next -> next.move(key, tab, column, row));
            }
            dragging = panning = false;
        }
        super.mouseMovedOrUp(x, y, button);
    }

    public void scroll(int x, int direction) {
        if (!menu.isEmpty()) menuScroll = Math.max(0, Math.min(menuScroll + direction, menu.size() - menuRows()));
        else if (x < left || x >= right) ResearchEditorClient.turnPage(direction);
        else geometry
            .thaumicdabblery$pan(geometry.thaumicdabblery$mapX(), geometry.thaumicdabblery$mapY() + direction * CELL);
    }

    private String tabAt(int x, int y) {
        int index = 0, count = ResearchEditorClient.tabsPerSide();
        for (String key : ResearchEditorClient.visibleTabs()) {
            int tx = index < count ? left - 40 : right + 16;
            int ty = top - 17 + (index % count) * 24;
            if (x >= tx && x < tx + 24 && y >= ty && y < ty + 24) return key;
            index++;
        }
        return null;
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (ResearchEditorClient.isToggleKey(key)) {
            ResearchEditorClient.done(tab);
            return;
        }
        if (key == Keyboard.KEY_ESCAPE) {
            if (!menu.isEmpty() || pick != Pick.NONE || dragging || panning) cancel();
            else mc.displayGuiScreen(null);
            return;
        }
        if (isCtrlKeyDown() && (key == Keyboard.KEY_Z || key == Keyboard.KEY_Y)) {
            cancel();
            history(key == Keyboard.KEY_Z && !isShiftKeyDown());
            return;
        }
        if (key == mc.gameSettings.keyBindInventory.getKeyCode()) mc.displayGuiScreen(null);
    }

    private void cancel() {
        menu.clear();
        dragging = panning = false;
        pick = Pick.NONE;
        source = previousParent = null;
    }

    private void startPick(Pick mode, String key) {
        pick = mode;
        source = key;
        notice = "Escape cancels. You can switch tabs while selecting.";
    }

    private void menu(String key, String title, int x, int y) {
        menu.clear();
        menuKey = key;
        menuTitle = title;
        menuWidth = Math.min(232, width - 12);
        menuX = Math.max(6, Math.min(x, width - menuWidth - 6));
        menuY = Math.max(30, Math.min(y, height - 166));
        menuScroll = 0;
    }

    private void rootMenu(String key, int x, int y) {
        menu(key, name(key), x, y);
        menu.add(new MenuItem("Parents...", () -> parentsMenu(key)));
        menu.add(new MenuItem("Move to tab...", () -> startPick(Pick.TAB, key)));
        menu.add(new MenuItem("Swap positions with...", () -> startPick(Pick.SWAP, key)));
        menu.add(new MenuItem("Properties...", () -> propertiesMenu(key)));
        menu.add(new MenuItem("Delete research", () -> {
            int[] count = { 0 };
            String label = name(key);
            edit("Deleted " + label, next -> count[0] = next.delete(key));
            if (!saveFailed) notice = "Deleted " + label + " and detached " + count[0] + " links. Undo to restore.";
        }));
        List<String> overlapping = new ArrayList<>();
        ResearchLayout.Entry chosen = layout.require(key);
        for (Map.Entry<String, ResearchLayout.Entry> pair : layout.entries.entrySet()) {
            ResearchLayout.Entry entry = pair.getValue();
            if (!entry.deleted && entry.tab.equals(chosen.tab) && entry.x == chosen.x && entry.y == chosen.y)
                overlapping.add(pair.getKey());
        }
        if (overlapping.size() > 1) menu.add(new MenuItem("Entries at this position...", () -> {
            menu(key, "Select overlapping entry", menuX, menuY);
            for (String other : overlapping) menu.add(new MenuItem(other, () -> {
                selected = other;
                rootMenu(other, menuX, menuY);
            }));
        }));
    }

    private void propertiesMenu(String key) {
        menu(key, "Properties: " + name(key), menuX, menuY);
        ResearchLayout.Entry entry = layout.require(key);
        for (int i = 0; i < ResearchLayout.FLAGS.length; i++) {
            final int index = i;
            menu.add(new MenuItem((entry.hasFlag(i) ? "[x] " : "[ ] ") + ResearchLayout.FLAGS[i], () -> {
                edit(
                    "Changed " + ResearchLayout.FLAGS[index] + " for " + name(key),
                    next -> next.require(key).flags ^= 1 << index);
                propertiesMenu(key);
            }));
        }
        menu.add(new MenuItem("< Back", () -> rootMenu(key, menuX, menuY)));
    }

    private void parentsMenu(String key) {
        menu(key, "Parents: " + name(key), menuX, menuY);
        ResearchLayout.Entry entry = layout.require(key);
        menu.add(new MenuItem("+ Add parent...", () -> {
            previousParent = null;
            hiddenLink = false;
            startPick(Pick.PARENT, key);
        }));
        for (String parent : ResearchLayout.links(entry.parents))
            menu.add(new MenuItem(name(parent), () -> parentMenu(key, parent, false)));
        for (String parent : ResearchLayout.links(entry.hidden))
            menu.add(new MenuItem("[hidden] " + name(parent), () -> parentMenu(key, parent, true)));
        menu.add(new MenuItem("< Back", () -> rootMenu(key, menuX, menuY)));
    }

    private void parentMenu(String child, String parent, boolean hidden) {
        menu(parent, "Parent: " + name(parent), menuX, menuY);
        menu.add(new MenuItem("Replace parent...", () -> {
            previousParent = parent;
            hiddenLink = hidden;
            startPick(Pick.PARENT, child);
        }));
        menu.add(new MenuItem("Remove parent", () -> {
            edit("Removed parent from " + name(child), next -> next.parent(child, parent, null, false));
            parentsMenu(child);
        }));
        menu.add(new MenuItem((hidden ? "[x] " : "[ ] ") + "Hidden link", () -> {
            edit("Changed parent link for " + name(child), next -> next.parent(child, parent, parent, !hidden));
            parentMenu(child, parent, !hidden);
        }));
        menu.add(new MenuItem("< Back", () -> parentsMenu(child)));
    }

    private int menuRows() {
        return Math.min(menu.size(), Math.max(1, (height - menuY - 38) / ROW));
    }

    private void drawMenu(int mx, int my) {
        int rows = menuRows(), bottom = menuY + 30 + rows * ROW;
        drawRect(menuX + 3, menuY + 3, menuX + menuWidth + 3, bottom + 3, 0x60000000);
        drawRect(menuX, menuY, menuX + menuWidth, bottom, 0xff242d3b);
        border(menuX, menuY, menuWidth, bottom - menuY, 0xff8898a6);
        drawString(fontRendererObj, fit(menuTitle, menuWidth - 10), menuX + 5, menuY + 4, 0xffffdf9c);
        drawString(fontRendererObj, fit(menuKey, menuWidth - 10), menuX + 5, menuY + 16, 0xff97a5b8);
        for (int i = 0; i < rows; i++) {
            int y = menuY + 30 + i * ROW;
            if (mx >= menuX && mx < menuX + menuWidth && my >= y && my < y + ROW)
                drawRect(menuX + 1, y, menuX + menuWidth - 1, y + ROW, 0xff465b70);
            drawString(
                fontRendererObj,
                fit(menu.get(menuScroll + i).label, menuWidth - 12),
                menuX + 6,
                y + 4,
                0xffeef0ec);
        }
        if (rows < menu.size()) drawString(fontRendererObj, "Scroll for more", menuX + 5, bottom + 4, 0xffdfc38c);
    }

    private static final class MenuItem {

        private final String label;
        private final Runnable action;

        private MenuItem(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }
    }
}
