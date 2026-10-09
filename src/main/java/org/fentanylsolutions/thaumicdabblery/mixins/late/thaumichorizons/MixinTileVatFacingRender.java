package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumichorizons;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import org.fentanylsolutions.thaumicdabblery.feature.vatfacing.VatFacingClient;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.kentington.thaumichorizons.client.renderer.tile.TileVatSlaveRender;

@Mixin(value = TileVatSlaveRender.class, remap = false)
public abstract class MixinTileVatFacingRender {

    @Redirect(
        method = { "renderTileEntityAt", "func_147500_a" },
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTranslatef(FFF)V"),
        require = 4)
    private void thaumicdabblery$bob(float tx, float ty, float tz, TileEntity tile, double x, double y, double z,
        float partial) {
        GL11.glTranslatef(tx, VatFacingClient.bobbingY(tile, ty, partial), tz);
    }

    @Redirect(
        method = { "renderTileEntityAt", "func_147500_a" },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderManager;renderEntitySimple(Lnet/minecraft/entity/Entity;F)Z",
            remap = true),
        require = 1)
    private boolean thaumicdabblery$face(RenderManager renderer, Entity entity, float partial, TileEntity tile,
        double x, double y, double z, float frame) {
        return VatFacingClient.render(tile, entity, partial, () -> renderer.renderEntitySimple(entity, partial));
    }

    @Redirect(
        method = { "renderTileEntityAt", "func_147500_a" },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderManager;func_147939_a(Lnet/minecraft/entity/Entity;DDDFFZ)Z",
            remap = true),
        require = 1)
    private boolean thaumicdabblery$faceDissolving(RenderManager renderer, Entity entity, double ex, double ey,
        double ez, float yaw, float partial, boolean debug, TileEntity tile, double x, double y, double z,
        float frame) {
        return VatFacingClient.render(
            tile,
            entity,
            partial,
            () -> renderer.func_147939_a(
                entity,
                ex,
                ey,
                ez,
                entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partial,
                partial,
                debug));
    }
}
