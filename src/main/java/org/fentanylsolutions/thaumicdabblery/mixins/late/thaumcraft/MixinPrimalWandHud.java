package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.customaspects.PrimalDiscovery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.lib.ClientTickEventsFML;
import thaumcraft.common.items.wands.ItemWandCasting;

@Mixin(value = ClientTickEventsFML.class, remap = false)
public abstract class MixinPrimalWandHud {

    @ModifyConstant(method = "renderCastingWandHud", constant = @Constant(intValue = 24))
    private int td$fitVisBars(int spacing, Float ticks, EntityPlayer player, long time, ItemStack held) {
        return Math.max(
            1,
            120 / Math.max(
                5,
                PrimalDiscovery.visiblePrimals(player)
                    .size() - 1));
    }

    @Redirect(
        method = "renderCastingWandHud",
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/common/items/wands/ItemWandCasting;getAllVis(Lnet/minecraft/item/ItemStack;)Lthaumcraft/api/aspects/AspectList;"))
    private AspectList td$visibleHud(ItemWandCasting wand, ItemStack stack, Float ticks, EntityPlayer player, long time,
        ItemStack held) {
        return PrimalDiscovery.visibleVis(wand.getAllVis(stack), player);
    }
}
