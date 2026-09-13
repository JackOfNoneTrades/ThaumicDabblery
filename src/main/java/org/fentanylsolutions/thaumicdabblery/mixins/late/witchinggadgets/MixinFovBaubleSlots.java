package org.fentanylsolutions.thaumicdabblery.mixins.late.witchinggadgets;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.oredict.OreDictionary;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleSlotsFeature;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.WitchingBaubleSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import baubles.api.BaublesApi;

@Mixin(targets = "witchinggadgets.client.ClientEventHandler", remap = false)
public abstract class MixinFovBaubleSlots {

    @Redirect(
        method = "onFOVUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/oredict/OreDictionary;itemMatches(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Z)Z"))
    private boolean td$sniperZoom(ItemStack expected, ItemStack actual, boolean strict, FOVUpdateEvent event) {
        return BaubleSlotsFeature.isEnabled()
            ? WitchingBaubleSlots.magic(BaublesApi.getBaubles(event.entity), 6) != null
            : OreDictionary.itemMatches(expected, actual, strict);
    }
}
