package org.fentanylsolutions.thaumicdabblery.feature.construct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.lib.UtilsFX;

/** Native-style exploded layers, with bounded scaling and a separately centered activation item. */
public final class ConstructPageRenderer {

    private static final RenderItem ITEMS = new RenderItem();
    private static final ResourceLocation OVERLAY = new ResourceLocation(
        "thaumcraft",
        "textures/gui/gui_researchbook_overlay.png");
    private static final Map<ConstructPage, Map<ItemStack, List<ItemStack>>> VARIANTS = new WeakHashMap<>();

    public static void draw(GuiResearchRecipe gui, ConstructPage page, int x, int y, int mx, int my) {
        Minecraft mc = Minecraft.getMinecraft();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            flat();
            centered(mc, StatCollector.translateToLocal("recipe.type.construct"), x + 66, y);
            if (page.activation != null) {
                ItemStack shown = variant(page, page.activation);
                item(mc, shown, x + 58, y + 18);
                if (hit(mx, my, x + 58, y + 18, 16)) tooltip(gui, mc, shown, mx, my, true);
            }
            float minY = -(page.width - 1) * 8;
            float width = (page.width + page.depth - 2) * 16 + 16;
            // Separate the full projected footprints, not just blocks at matching coordinates.
            // Otherwise the front of an upper layer appears embedded in the back of the layer below.
            int layerHeight = (page.width + page.depth - 2) * 8 + 16;
            int layerSpacing = Math.max(32, layerHeight + 8);
            float height = layerHeight + (page.height - 1) * layerSpacing;
            float top = page.activation == null ? 24 : 44;
            float bottom = page.cost.size() > 0 ? 146 : 176;
            float scale = Math.min(1.25F, Math.min(116 / width, (bottom - top) / height));
            float ox = x + 66 - width * scale / 2;
            float oy = y + top + (bottom - top - height * scale) / 2 - minY * scale;
            mc.getTextureManager()
                .bindTexture(OVERLAY);
            GL11.glColor4f(1, 1, 1, .35F);
            quad(x + 2, y + bottom - 50, 128, 64, 0, 72 / 256D, 64 / 256D, 116 / 256D);
            GL11.glColor4f(1, 1, 1, 1);
            ItemStack hovered = null;
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(ox, oy, 0);
                GL11.glScalef(scale, scale, scale);
                for (int layer = page.height - 1; layer >= 0; layer--) {
                    for (int row = page.depth - 1; row >= 0; row--) for (int col = page.width - 1; col >= 0; col--) {
                        ItemStack cell = page.layers[layer][row][col];
                        if (cell == null) continue;
                        ItemStack shown = variant(page, cell);
                        int px = col * 16 + row * 16;
                        int py = -col * 8 + row * 8 + (page.height - 1 - layer) * layerSpacing;
                        GL11.glPushMatrix();
                        try {
                            GL11.glTranslatef(0, 0, 60 - (page.height - 1 - layer) * 10);
                            Block block = Block.getBlockFromItem(shown.getItem());
                            if (block instanceof BlockLiquid) liquid(mc, block, shown.getItemDamage(), px, py);
                            else item(mc, shown, px, py);
                        } finally {
                            GL11.glPopMatrix();
                        }
                        if (hit(mx, my, ox + px * scale, oy + py * scale, 16 * scale) && hovered == null)
                            hovered = shown;
                    }
                }
            } finally {
                GL11.glPopMatrix();
            }
            if (hovered != null) tooltip(gui, mc, hovered, mx, my, false);
            if (page.cost.size() > 0) {
                Aspect[] aspects = page.cost.getAspects();
                Arrays.sort(aspects, java.util.Comparator.comparing(Aspect::getTag));
                int groups = (aspects.length + 5) / 6;
                int first = (mc.thePlayer.ticksExisted / 60 % groups) * 6;
                int count = Math.min(6, aspects.length - first);
                for (int i = 0; i < count; i++) {
                    Aspect aspect = aspects[first + i];
                    int ax = x + 66 - count * 22 / 2 + i * 22 + 3;
                    UtilsFX.drawTag(ax, y + 154, aspect, 0, 0, 0, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, false);
                    flat();
                    centered(mc, Integer.toString(page.cost.getAmount(aspect)), ax + 8, y + 172);
                    if (hit(mx, my, ax, y + 154, 16))
                        gui.drawCustomTooltip(gui, ITEMS, mc.fontRenderer, Arrays.asList(aspect.getName()), mx, my, 11);
                }
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private static ItemStack variant(ConstructPage page, ItemStack stack) {
        if (stack.getItemDamage() != 32767) return stack;
        Map<ItemStack, List<ItemStack>> cache = VARIANTS.computeIfAbsent(page, key -> new IdentityHashMap<>());
        List<ItemStack> variants = cache.get(stack);
        if (variants == null) {
            List<ItemStack> listed = new ArrayList<>();
            stack.getItem()
                .getSubItems(
                    stack.getItem(),
                    stack.getItem()
                        .getCreativeTab(),
                    listed);
            variants = new ArrayList<>();
            for (ItemStack entry : listed) {
                if (entry == null || entry.getItem() != stack.getItem()
                    || entry.getItemDamage() < 0
                    || entry.getItemDamage() == 32767) continue;
                ItemStack copy = stack.copy();
                copy.setItemDamage(entry.getItemDamage());
                variants.add(copy);
            }
            if (variants.isEmpty()) {
                ItemStack copy = stack.copy();
                copy.setItemDamage(0);
                variants.add(copy);
            }
            cache.put(stack, variants);
        }
        return variants.get((int) (System.currentTimeMillis() / 1000 % variants.size()));
    }

    private static boolean hit(int mx, int my, float x, float y, float size) {
        return mx >= x && my >= y && mx < x + size && my < y + size;
    }

    private static void tooltip(GuiResearchRecipe gui, Minecraft mc, ItemStack stack, int mx, int my,
        boolean activation) {
        List<String> text = new ArrayList<>(stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips));
        if (activation) text.add(StatCollector.translateToLocal("thaumicdabblery.construct.apply"));
        gui.drawCustomTooltip(gui, ITEMS, mc.fontRenderer, text, mx, my, 11);
    }

    private static void centered(Minecraft mc, String text, int x, int y) {
        mc.fontRenderer.drawString(text, x - mc.fontRenderer.getStringWidth(text) / 2, y, 0x505050);
    }

    private static void flat() {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1, 1, 1, 1);
    }

    private static void item(Minecraft mc, ItemStack stack, int x, int y) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(0, 0, 100);
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            ITEMS.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private static void liquid(Minecraft mc, Block block, int metadata, int x, int y) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            flat();
            // These GUI faces are double-sided, regardless of the caller's model culling state.
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glTranslatef(x, y, 100);
            mc.getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
            IIcon icon = block.getIcon(1, metadata);
            int color = block.getRenderColor(metadata);
            face(icon, color, 1, new int[] { 0, 4, 8, 0, 16, 4, 8, 8 });
            face(icon, color, .6F, new int[] { 0, 4, 8, 8, 8, 16, 0, 12 });
            face(icon, color, .8F, new int[] { 8, 8, 16, 4, 16, 12, 8, 16 });
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private static void face(IIcon icon, int color, float shade, int[] xy) {
        GL11.glColor4f(
            (color >> 16 & 255) / 255F * shade,
            (color >> 8 & 255) / 255F * shade,
            (color & 255) / 255F * shade,
            1);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(xy[0], xy[1], 0, icon.getMinU(), icon.getMinV());
        t.addVertexWithUV(xy[2], xy[3], 0, icon.getMaxU(), icon.getMinV());
        t.addVertexWithUV(xy[4], xy[5], 0, icon.getMaxU(), icon.getMaxV());
        t.addVertexWithUV(xy[6], xy[7], 0, icon.getMinU(), icon.getMaxV());
        t.draw();
    }

    private static void quad(float x, float y, float width, float height, double u0, double v0, double u1, double v1) {
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(x, y + height, 0, u0, v1);
        t.addVertexWithUV(x + width, y + height, 0, u1, v1);
        t.addVertexWithUV(x + width, y, 0, u1, v0);
        t.addVertexWithUV(x, y, 0, u0, v0);
        t.draw();
    }
}
