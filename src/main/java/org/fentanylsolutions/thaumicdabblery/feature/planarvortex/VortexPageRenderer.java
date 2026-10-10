package org.fentanylsolutions.thaumicdabblery.feature.planarvortex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.config.ConfigItems;

public final class VortexPageRenderer {

    private static final RenderItem ITEMS = new RenderItem();
    private static final ResourceLocation VORTEX = new ResourceLocation("thaumcraft", "textures/misc/nodes.png");
    private static final ResourceLocation BOOK = new ResourceLocation(
        "thaumcraft",
        "textures/gui/gui_researchbook.png");
    private static final int INK = 0x403047;

    public static void draw(GuiResearchRecipe gui, VortexPage page, int x, int y, int mx, int my) {
        VortexRecipes.Recipe recipe = page.resolve();
        if (recipe == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            flat();
            centered(font, tr("title"), x + 66, y, 132);
            mc.getTextureManager()
                .bindTexture(BOOK);
            // The stock book's forward arrow, in its native 256-unit texture coordinates.
            quad(x + 28, y + 50, 18, 12, 12 / 256D, 184 / 256D, 24 / 256D, 192 / 256D);
            quad(x + 86, y + 50, 18, 12, 12 / 256D, 184 / 256D, 24 / 256D, 192 / 256D);
            if (!"instant".equals(recipe.completion)) {
                item(mc, new ItemStack(ConfigItems.itemWandCasting), x + 58, y + 16);
                if (hover(mx, my, x + 58, y + 16)) tooltip(gui, font, Arrays.asList(tr("wand")), mx, my);
            }
            mc.getTextureManager()
                .bindTexture(VORTEX);
            // TileVortexRender uses row 2 of TC's 32-frame node sheet for a stabilized vortex.
            int frame = (int) (System.nanoTime() / 40000000L % 32);
            quad(x + 46, y + 36, 40, 40, frame / 32D, 2 / 32D, (frame + 1) / 32D, 3 / 32D);
            ItemStack input = recipe.input.copy();
            if (input.getItemDamage() == 32767) input.setItemDamage(0);
            item(mc, input, x + 8, y + 48);
            if (hover(mx, my, x + 8, y + 48)) {
                List<String> inputTip = new ArrayList<>(
                    input.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips));
                if (recipe.input.getItemDamage() == 32767) inputTip.add(tr("wildcard"));
                if (recipe.input.hasTagCompound()) {
                    inputTip.add(tr("exact_nbt"));
                    inputTip.addAll(
                        font.listFormattedStringToWidth(
                            recipe.input.getTagCompound()
                                .toString(),
                            220));
                }
                tooltip(gui, font, inputTip, mx, my);
            }
            ItemStack icon = page.outputIcon != null ? page.outputIcon : recipe.output;
            boolean model = icon == null;
            if (model && !VortexEntityPreview.draw(gui, page, recipe, x + 116, y + 56)) {
                icon = new ItemStack(Items.spawn_egg);
            }
            if (icon != null) {
                icon = icon.copy();
                if (recipe.output != null) icon.stackSize = recipe.output.stackSize;
                item(mc, icon, x + 108, y + 48);
            }
            String name = recipe.output != null ? recipe.output.getDisplayName() : entityName(recipe);
            if ("builtin:wisps".equals(page.key)) name = tr("wisps");
            if ("builtin:void_golem".equals(page.key)) name = tr("void_golem");
            if (model ? mx >= x + 101 && mx < x + 132 && my >= y + 36 && my < y + 76 : hover(mx, my, x + 108, y + 48)) {
                List<String> outputTip = recipe.output != null
                    ? new ArrayList<>(recipe.output.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips))
                    : new ArrayList<>(Arrays.asList(name));
                if (recipe.entity != null && !recipe.nbt.hasNoTags()) {
                    outputTip.add(tr("entity_nbt"));
                    outputTip.addAll(font.listFormattedStringToWidth(recipe.nbt.toString(), 220));
                }
                tooltip(gui, font, outputTip, mx, my);
            }
            flat();
            centered(font, name, x + 66, y + 85, 132);
            if ("builtin:void_golem".equals(page.key)) centered(font, tr("owner"), x + 66, y + 117, 132);
            int count = recipe.vis.size();
            if (count > 0) {
                Aspect[] aspects = recipe.vis.getAspects();
                Arrays.sort(aspects, java.util.Comparator.comparing(Aspect::getTag));
                int pages = (count + 5) / 6, current = mc.thePlayer.ticksExisted / 60 % pages;
                centered(
                    font,
                    tr("vis") + (pages > 1 ? " (" + (current + 1) + "/" + pages + ")" : ""),
                    x + 66,
                    y + 107,
                    132);
                int start = current * 6, shown = Math.min(6, count - start);
                for (int i = 0; i < shown; i++) {
                    Aspect aspect = aspects[start + i];
                    int ax = x + 66 - shown * 22 / 2 + i * 22 + 3;
                    UtilsFX.drawTag(ax, y + 122, aspect, 0, 0, 0, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, false);
                    flat();
                    centered(font, Integer.toString(recipe.vis.getAmount(aspect)), ax + 8, y + 142, 22);
                    if (hover(mx, my, ax, y + 122))
                        tooltip(gui, font, Arrays.asList(aspect.getName(), tr("discount")), mx, my);
                }
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private static void quad(int x, int y, int width, int height, double u0, double v0, double u1, double v1) {
        GL11.glColor4f(1, 1, 1, 1);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(x, y + height, 0, u0, v1);
        t.addVertexWithUV(x + width, y + height, 0, u1, v1);
        t.addVertexWithUV(x + width, y, 0, u1, v0);
        t.addVertexWithUV(x, y, 0, u0, v0);
        t.draw();
    }

    private static String entityName(VortexRecipes.Recipe recipe) {
        if (recipe.nbt.hasKey("CustomName")) return recipe.nbt.getString("CustomName");
        String key = "entity." + recipe.entity + ".name";
        String translated = StatCollector.translateToLocal(key);
        return translated.equals(key) ? recipe.entity : translated;
    }

    private static String tr(String key) {
        return StatCollector.translateToLocal("thaumicdabblery.vortex.page." + key);
    }

    private static boolean hover(int mx, int my, int x, int y) {
        return mx >= x && mx < x + 16 && my >= y && my < y + 16;
    }

    private static void tooltip(GuiResearchRecipe gui, FontRenderer font, List<String> lines, int mx, int my) {
        gui.drawCustomTooltip(gui, ITEMS, font, lines, mx, my, 11);
    }

    private static void flat() {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1, 1, 1, 1);
    }

    private static void centered(FontRenderer font, String text, int cx, int y, int width) {
        float scale = Math.min(1, width / (float) Math.max(1, font.getStringWidth(text)));
        GL11.glPushMatrix();
        GL11.glTranslatef(cx, y, 0);
        GL11.glScalef(scale, scale, 1);
        font.drawString(text, -font.getStringWidth(text) / 2, 0, INK);
        GL11.glPopMatrix();
    }

    private static void item(Minecraft mc, ItemStack stack, int x, int y) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(0, 0, 100);
            GL11.glColor4f(1, 1, 1, 1);
            // RenderItem scales 3D blocks; vanilla GuiContainer enables this for correct face lighting.
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            ITEMS.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
            ITEMS.renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
}
