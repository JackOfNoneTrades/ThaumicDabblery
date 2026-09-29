package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.customaspects.PrimalDiscovery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.baubles.ItemAmuletVis;
import thaumcraft.common.items.wands.ItemWandCasting;

@Mixin(value = { ItemWandCasting.class, ItemAmuletVis.class }, remap = false)
public abstract class MixinPrimalWandTooltip {

    @Redirect(
        method = "addInformation",
        remap = true,
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/api/aspects/Aspect;getPrimalAspects()Ljava/util/ArrayList;",
            remap = false))
    private ArrayList<Aspect> td$knownVis(ItemStack stack, EntityPlayer player, List<?> lines, boolean advanced) {
        return PrimalDiscovery.visiblePrimals(player);
    }
}
