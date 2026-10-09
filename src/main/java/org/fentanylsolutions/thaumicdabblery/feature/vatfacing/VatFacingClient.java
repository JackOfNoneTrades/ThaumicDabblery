package org.fentanylsolutions.thaumicdabblery.feature.vatfacing;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetHandler;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import com.kentington.thaumichorizons.common.tiles.TileVat;

public final class VatFacingClient {

    public static void receive(VatFacingNetwork.Pose pose, INetHandler source) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.func_152344_a(() -> {
            if (mc.getNetHandler() != source || mc.theWorld == null
                || mc.theWorld.provider.dimensionId != pose.dimension
                || pose.data == null) return;
            TileEntity tile = mc.theWorld.getTileEntity(pose.x, pose.y, pose.z);
            if (tile instanceof TileVat) VatFacing.state((TileVat) tile)
                .read(pose.data);
        });
    }

    public static VatFacing.State state(TileEntity interior) {
        TileEntity tile = interior.getWorldObj()
            .getTileEntity(interior.xCoord, interior.yCoord + 1, interior.zCoord);
        return tile instanceof TileVat ? VatFacing.state((TileVat) tile) : null;
    }

    /** Replace the native wave without moving the contained entity or changing the sample-item branch. */
    public static float bobbingY(TileEntity interior, float nativeY, float partial) {
        VatFacing.State state = state(interior);
        if (state == null || !state.customBobbing) return nativeY;
        float original = 0.1F * (float) Math.cos(Math.toRadians(Minecraft.getMinecraft().thePlayer.ticksExisted));
        TileVat vat = (TileVat) interior.getWorldObj()
            .getTileEntity(interior.xCoord, interior.yCoord + 1, interior.zCoord);
        float wave = VatAppearance.bob(
            state,
            interior.getWorldObj()
                .getTotalWorldTime(),
            partial);
        // Effigies have already been rotated 180 degrees about Z by Horizons.
        return nativeY - original + (vat.getEntityContained() == null ? -wave : wave);
    }

    public static boolean render(TileEntity interior, Entity entity, float partial, Supplier<Boolean> draw) {
        VatFacing.State state = state(interior);
        if (state == null || !(entity instanceof EntityLivingBase) || entity instanceof EntityPlayer) return draw.get();
        boolean transform = state.yOffset != 0 || state.scale != 1;
        if (!transform) return renderFacing(state, entity, partial, draw);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(0, state.yOffset, 0);
            // Scale around the creature's rendered origin, not the camera or world origin.
            double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partial - RenderManager.renderPosX;
            double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partial - RenderManager.renderPosY;
            double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partial - RenderManager.renderPosZ;
            GL11.glTranslated(x, y, z);
            GL11.glScalef(state.scale, state.scale, state.scale);
            GL11.glTranslated(-x, -y, -z);
            return renderFacing(state, entity, partial, draw);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static boolean renderFacing(VatFacing.State state, Entity entity, float partial, Supplier<Boolean> draw) {
        if (!state.active) return draw.get();
        EntityLivingBase mob = (EntityLivingBase) entity;
        float yaw = mob.rotationYaw, prevYaw = mob.prevRotationYaw, body = mob.renderYawOffset,
            prevBody = mob.prevRenderYawOffset;
        float head = mob.rotationYawHead, prevHead = mob.prevRotationYawHead, pitch = mob.rotationPitch,
            prevPitch = mob.prevRotationPitch;
        try {
            float facing = VatFacing.interpolate(state.prevBody, state.body, partial);
            mob.rotationYaw = mob.prevRotationYaw = mob.renderYawOffset = mob.prevRenderYawOffset = facing;
            mob.rotationYawHead = mob.prevRotationYawHead = facing
                + VatFacing.interpolate(state.prevHead, state.head, partial);
            mob.rotationPitch = mob.prevRotationPitch = VatFacing.interpolate(state.prevPitch, state.pitch, partial);
            return draw.get();
        } finally {
            mob.rotationYaw = yaw;
            mob.prevRotationYaw = prevYaw;
            mob.renderYawOffset = body;
            mob.prevRenderYawOffset = prevBody;
            mob.rotationYawHead = head;
            mob.prevRotationYawHead = prevHead;
            mob.rotationPitch = pitch;
            mob.prevRotationPitch = prevPitch;
        }
    }
}
