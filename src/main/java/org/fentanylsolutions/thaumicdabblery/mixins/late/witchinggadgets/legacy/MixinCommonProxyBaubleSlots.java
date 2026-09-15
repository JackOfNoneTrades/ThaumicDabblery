package org.fentanylsolutions.thaumicdabblery.mixins.late.witchinggadgets.legacy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleSlotsFeature;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.LegacyWitchingBaubles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "witchinggadgets.common.CommonProxy", remap = false)
public abstract class MixinCommonProxyBaubleSlots {

    @Inject(method = "getServerGuiElement", at = @At("HEAD"), cancellable = true)
    private void td$storage(int id, EntityPlayer player, World world, int x, int y, int z,
        CallbackInfoReturnable<Object> cir) {
        if (!BaubleSlotsFeature.isEnabled() || (id != 4 && id != 5)) return;
        ItemStack stack = LegacyWitchingBaubles.storage(player, id, x, y);
        cir.setReturnValue(
            stack == null ? null : new witchinggadgets.common.gui.ContainerCloak(player.inventory, world, stack));
    }
}
