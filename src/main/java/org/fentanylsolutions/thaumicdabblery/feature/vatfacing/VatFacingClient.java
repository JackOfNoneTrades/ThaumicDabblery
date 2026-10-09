package org.fentanylsolutions.thaumicdabblery.feature.vatfacing;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.INetHandler;
import net.minecraft.tileentity.TileEntity;

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

    public static boolean render(TileEntity interior, Entity entity, float partial, Supplier<Boolean> draw) {
        VatFacing.State state = state(interior);
        if (state == null || !state.active || !(entity instanceof EntityLivingBase)) return draw.get();
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
