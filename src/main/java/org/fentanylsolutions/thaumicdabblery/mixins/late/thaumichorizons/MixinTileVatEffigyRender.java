package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumichorizons;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import org.fentanylsolutions.thaumicdabblery.feature.effigyskins.EffigySkinClient;
import org.fentanylsolutions.thaumicdabblery.feature.vatfacing.VatFacing;
import org.fentanylsolutions.thaumicdabblery.feature.vatfacing.VatFacingClient;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.kentington.thaumichorizons.client.renderer.tile.TileVatSlaveRender;

@Mixin(value = TileVatSlaveRender.class, remap = false)
public abstract class MixinTileVatEffigyRender {

    @Redirect(
        method = { "renderTileEntityAt", "func_147500_a" },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/ModelBiped;render(Lnet/minecraft/entity/Entity;FFFFFF)V",
            remap = true),
        require = 2)
    private void thaumicdabblery$renderSkin(ModelBiped model, Entity entity, float limb, float amount, float age,
        float yaw, float pitch, float scale, TileEntity tile, double x, double y, double z, float partialTicks) {
        VatFacing.State state = VatFacingClient.state(tile);
        GL11.glPushMatrix();
        try {
            if (state != null) {
                GL11.glTranslatef(0, -state.yOffset, 0);
                GL11.glScalef(state.scale, state.scale, state.scale);
            }
            if (state != null && state.active) {
                GL11.glRotatef(VatFacing.interpolate(state.prevBody, state.body, partialTicks) - 180, 0, 1, 0);
                yaw = VatFacing.interpolate(state.prevHead, state.head, partialTicks);
                pitch = VatFacing.interpolate(state.prevPitch, state.pitch, partialTicks);
            }
            if (!EffigySkinClient.render(tile, scale, yaw, pitch))
                model.render(entity, limb, amount, age, yaw, pitch, scale);
        } finally {
            GL11.glPopMatrix();
        }
    }
}
