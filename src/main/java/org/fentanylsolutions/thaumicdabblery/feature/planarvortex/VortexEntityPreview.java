package org.fentanylsolutions.thaumicdabblery.feature.planarvortex;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;
import org.lwjgl.opengl.GL11;

import com.kentington.thaumichorizons.common.entities.EntityGolemTH;

import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.common.entities.monster.EntityWisp;

/** Unspawned, unticked models owned by the open book, never by the recipe registry. */
final class VortexEntityPreview {

    private static final Map<GuiResearchRecipe, Map<VortexPage, Preview>> CACHE = new WeakHashMap<>();

    private static final class Preview {

        final String type;
        final NBTTagCompound nbt;
        EntityLiving entity;
        boolean failed;

        Preview(VortexRecipes.Recipe recipe) {
            type = recipe.entity;
            nbt = (NBTTagCompound) recipe.nbt.copy();
        }
    }

    static boolean draw(GuiResearchRecipe gui, VortexPage page, VortexRecipes.Recipe recipe, int cx, int cy) {
        Minecraft mc = Minecraft.getMinecraft();
        Map<VortexPage, Preview> previews = CACHE.computeIfAbsent(gui, key -> new HashMap<>());
        Preview preview = previews.get(page);
        if (preview == null || !preview.type.equals(recipe.entity)
            || !preview.nbt.equals(recipe.nbt)
            || (preview.entity != null && preview.entity.worldObj != mc.theWorld)) {
            preview = new Preview(recipe);
            previews.put(page, preview);
        }
        if (preview.failed) return false;
        try {
            if (preview.entity == null) {
                preview.entity = (EntityLiving) EntityList.createEntityByName(recipe.entity, mc.theWorld);
                if (preview.entity == null) throw new IllegalArgumentException("Unknown entity " + recipe.entity);
                if ("builtin:void_golem".equals(page.key)) {
                    ((EntityGolemTH) preview.entity).loadGolem(0, 0, 0, null, 0, -420, false, false, false);
                }
                if ("builtin:wisps".equals(page.key)) ((EntityWisp) preview.entity).setType("aer");
                if (!recipe.nbt.hasNoTags()) {
                    NBTTagCompound data = new NBTTagCompound();
                    preview.entity.writeToNBT(data);
                    VortexRecipes.merge(data, recipe.nbt);
                    preview.entity.readFromNBT(data);
                }
                preview.entity.setAlwaysRenderNameTag(false);
            }
            render(preview.entity, mc, cx, cy);
            return true;
        } catch (RuntimeException | LinkageError error) {
            preview.failed = true;
            ThaumicDabblery.LOG.error("Unable to render vortex recipe preview " + page.key, error);
            return false;
        }
    }

    private static void render(EntityLiving entity, Minecraft mc, int cx, int cy) {
        RenderManager manager = RenderManager.instance;
        Render renderer = manager.getEntityRenderObject(entity);
        if (renderer == null) throw new IllegalArgumentException(
            "No renderer for " + entity.getClass()
                .getName());
        float yaw = manager.playerViewY, pitch = manager.playerViewX;
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        float rx = ActiveRenderInfo.rotationX, rxz = ActiveRenderInfo.rotationXZ;
        float rz = ActiveRenderInfo.rotationZ, ryz = ActiveRenderInfo.rotationYZ, rxy = ActiveRenderInfo.rotationXY;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            boolean wisp = entity instanceof EntityWisp;
            float scale = wisp ? 12
                : Math.min(22, Math.min(28 / Math.max(.1F, entity.width * 1.8F), 36 / Math.max(.1F, entity.height)));
            GL11.glTranslatef(cx, cy + (wisp ? .45F : entity.height / 2) * scale, 100);
            GL11.glScalef(-scale, scale, scale);
            GL11.glRotatef(180, 0, 0, 1);
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glRotatef(135, 0, 1, 0);
            RenderHelper.enableStandardItemLighting();
            GL11.glRotatef(-135, 0, 1, 0);
            if (wisp) {
                // GUI mirroring reverses billboard winding; wisps do not disable culling themselves.
                GL11.glDisable(GL11.GL_CULL_FACE);
                GL11.glDisable(GL11.GL_LIGHTING);
            } else GL11.glRotatef(30, 0, 1, 0);
            entity.ticksExisted = mc.thePlayer.ticksExisted;
            entity.prevRenderYawOffset = entity.renderYawOffset = 0;
            entity.prevRotationYaw = entity.rotationYaw = 0;
            entity.prevRotationYawHead = entity.rotationYawHead = 0;
            entity.prevRotationPitch = entity.rotationPitch = 0;
            manager.playerViewY = 180;
            manager.playerViewX = 0;
            ActiveRenderInfo.rotationX = ActiveRenderInfo.rotationXZ = 1;
            ActiveRenderInfo.rotationZ = ActiveRenderInfo.rotationYZ = ActiveRenderInfo.rotationXY = 0;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            // Calling the renderer directly avoids world shadows, fire and debug bounding boxes.
            renderer.doRender(entity, 0, 0, 0, 0, 1);
        } finally {
            manager.playerViewY = yaw;
            manager.playerViewX = pitch;
            ActiveRenderInfo.rotationX = rx;
            ActiveRenderInfo.rotationXZ = rxz;
            ActiveRenderInfo.rotationZ = rz;
            ActiveRenderInfo.rotationYZ = ryz;
            ActiveRenderInfo.rotationXY = rxy;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
}
