package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumichorizons;

import net.minecraft.tileentity.TileEntity;

import org.fentanylsolutions.thaumicdabblery.feature.planarvortex.VortexCraftingClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.kentington.thaumichorizons.client.renderer.tile.TileVortexRender;
import com.kentington.thaumichorizons.common.tiles.TileVortex;

@Mixin(value = TileVortexRender.class, remap = false)
public abstract class MixinTileVortexCraftingRender {

    @Inject(method = "renderTileEntityAt", at = @At("HEAD"), cancellable = true, remap = true, require = 1)
    private void thaumicdabblery$crafting(TileEntity tile, double x, double y, double z, float partialTicks,
        CallbackInfo ci) {
        if (tile instanceof TileVortex && VortexCraftingClient.render((TileVortex) tile, x, y, z, partialTicks))
            ci.cancel();
    }

    @ModifyArg(
        method = "renderNode",
        at = @At(value = "INVOKE", target = "Lthaumcraft/client/lib/UtilsFX;renderFacingStrip(DDDFFFIIIFI)V"),
        index = 5,
        require = 1)
    private static float thaumicdabblery$haloBrightness(float alpha) {
        return alpha * VortexCraftingClient.brightness;
    }

    @ModifyArg(
        method = "renderNode",
        at = @At(
            value = "INVOKE",
            target = "Lcom/kentington/thaumichorizons/client/renderer/tile/TileVortexRender;renderVortex(DDDFFFFI)V"),
        index = 5,
        require = 1)
    private static float thaumicdabblery$coreBrightness(float alpha) {
        return alpha * VortexCraftingClient.brightness;
    }
}
