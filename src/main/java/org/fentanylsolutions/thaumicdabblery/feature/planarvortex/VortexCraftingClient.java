package org.fentanylsolutions.thaumicdabblery.feature.planarvortex;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import com.kentington.thaumichorizons.client.renderer.tile.TileVortexRender;
import com.kentington.thaumichorizons.common.tiles.TileVortex;

public final class VortexCraftingClient {

    public static float brightness = 1;

    public static void receive(Minecraft mc, NBTTagCompound data) {
        if (mc.theWorld == null || mc.theWorld.provider.dimensionId != data.getInteger("dimension")) return;
        TileEntity tile = mc.theWorld.getTileEntity(data.getInteger("x"), data.getInteger("y"), data.getInteger("z"));
        if (tile instanceof TileVortex) {
            TileVortex vortex = (TileVortex) tile;
            VortexCrafting.releaseInput(vortex);
            VortexCrafting.State state = VortexCrafting.state(vortex);
            state.started = data.getLong("craftStarted");
            if (state.started >= 0 && data.hasKey("inputId")) {
                state.inputId = data.getInteger("inputId");
                state.suction = new VortexSuction.Path(
                    state.started,
                    data.getDouble("inputX"),
                    data.getDouble("inputY"),
                    data.getDouble("inputZ"),
                    tile.xCoord + .5,
                    tile.yCoord + .5,
                    tile.zCoord + .5);
                VortexCrafting.tick(vortex);
            }
        }
    }

    public static float age(TileVortex tile, float partialTicks) {
        long start = VortexCrafting.state(tile).started;
        return start < 0 ? -1
            : (float) (tile.getWorldObj()
                .getTotalWorldTime() - start) + partialTicks;
    }

    public static boolean render(TileVortex tile, double x, double y, double z, float partialTicks) {
        float age = age(tile, partialTicks);
        if (!tile.clientSynced || !VortexCrafting.ready(tile) || age < 0 || age >= VortexCrafting.DURATION)
            return false;
        float previous = brightness;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        try {
            brightness = VortexCrafting.brightness(age);
            TileVortexRender.renderNode(
                Minecraft.getMinecraft().renderViewEntity,
                64,
                true,
                false,
                10 * VortexCrafting.scale(age),
                tile.xCoord,
                tile.yCoord,
                tile.zCoord,
                partialTicks,
                tile.aspects,
                tile.count,
                tile.collapsing,
                tile.beams,
                tile.createdDimension,
                tile.cheat);
            brightness = previous;
            GL11.glTranslated(x + .5, y + .5, z + .5);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glDepthMask(false);
            GL11.glShadeModel(GL11.GL_SMOOTH);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            float envelope = VortexCrafting.rays(age);
            if (!org.fentanylsolutions.thaumicdabblery.Config.showVortexCraftingRays || envelope <= 0) return true;
            Random random = new Random(31100L + tile.xCoord * 31L + tile.yCoord * 17L + tile.zCoord);
            for (int i = 0; i < 14; i++) {
                GL11.glPushMatrix();
                GL11.glRotatef(random.nextFloat() * 360 + age * (i % 2 == 0 ? 2 : -2), 1, 0, 0);
                GL11.glRotatef(random.nextFloat() * 360 + age * 3, 0, 1, 0);
                GL11.glRotatef(random.nextFloat() * 360 + age * 4, 0, 0, 1);
                float length = (.75F + random.nextFloat() * 1.5F) * envelope;
                float width = (.15F + random.nextFloat() * .3F) * envelope;
                Tessellator t = Tessellator.instance;
                t.startDrawing(GL11.GL_TRIANGLE_FAN);
                t.setColorRGBA(245, 225, 255, (int) (210 * envelope));
                t.addVertex(0, 0, 0);
                t.setColorRGBA(135, 45, 235, 0);
                t.addVertex(-.866 * width, length, -.5 * width);
                t.addVertex(.866 * width, length, -.5 * width);
                t.addVertex(0, length, width);
                t.addVertex(-.866 * width, length, -.5 * width);
                t.draw();
                GL11.glPopMatrix();
            }
        } finally {
            brightness = previous;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
        return true;
    }
}
