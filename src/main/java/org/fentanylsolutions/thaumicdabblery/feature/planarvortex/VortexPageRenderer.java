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
    private static final ResourceLocation DIAGRAM = new ResourceLocation(
        "thaumicdabblery",
        "textures/gui/vortex_recipe.png");
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
                .bindTexture(DIAGRAM);
            // Like the stock smelting page: offering over the diagram, result beneath its arrow.
            quad(x + 9, y - 4, 114, 137, 0, 0, 1, 1);
            if (!"instant".equals(recipe.completion)) {
                item(mc, new ItemStack(ConfigItems.itemWandCasting), x + 18, y + 52);
                if (hover(mx, my, x + 18, y + 52)) tooltip(gui, font, Arrays.asList(tr("wand")), mx, my);
            }
            ItemStack input = recipe.input.copy();
            if (input.getItemDamage() == 32767) input.setItemDamage(0);
            item(mc, input, x + 58, y + 18);
            if (hover(mx, my, x + 58, y + 18)) {
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
            if (model && !VortexEntityPreview.draw(gui, page, recipe, x + 66, y + 120)) {
                icon = new ItemStack(Items.spawn_egg);
            }
            if (icon != null) {
                icon = icon.copy();
                if (recipe.output != null) icon.stackSize = recipe.output.stackSize;
                item(mc, icon, x + 58, y + 112);
            }
            String name = recipe.output != null ? recipe.output.getDisplayName() : entityName(recipe);
            if ("builtin:wisps".equals(page.key)) name = tr("wisps");
            if ("builtin:void_golem".equals(page.key)) name = tr("void_golem");
            if (model ? mx >= x + 51 && mx < x + 82 && my >= y + 100 && my < y + 140 : hover(mx, my, x + 58, y + 112)) {
                List<String> outputTip = recipe.output != null
                    ? new ArrayList<>(recipe.output.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips))
                    : new ArrayList<>(Arrays.asList(name));
                if (recipe.entity != null && !recipe.nbt.hasNoTags()) {
                    outputTip.add(tr("entity_nbt"));
                    outputTip.addAll(font.listFormattedStringToWidth(recipe.nbt.toString(), 220));
                }
                if ("builtin:void_golem".equals(page.key)) outputTip.add(tr("owner"));
                tooltip(gui, font, outputTip, mx, my);
            }
            flat();
            int count = recipe.vis.size();
            if (count > 0) {
                Aspect[] aspects = recipe.vis.getAspects();
                Arrays.sort(aspects, java.util.Comparator.comparing(Aspect::getTag));
                int pages = (count + 5) / 6, current = mc.thePlayer.ticksExisted / 60 % pages;
                if (pages > 1) centered(font, (current + 1) + "/" + pages, x + 66, y + 142, 132);
                int start = current * 6, shown = Math.min(6, count - start);
                for (int i = 0; i < shown; i++) {
                    Aspect aspect = aspects[start + i];
                    int ax = x + 66 - shown * 22 / 2 + i * 22 + 3;
                    UtilsFX.drawTag(ax, y + 154, aspect, 0, 0, 0, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, false);
                    flat();
                    centered(font, Integer.toString(recipe.vis.getAmount(aspect)), ax + 8, y + 172, 22);
                    if (hover(mx, my, ax, y + 154))
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
