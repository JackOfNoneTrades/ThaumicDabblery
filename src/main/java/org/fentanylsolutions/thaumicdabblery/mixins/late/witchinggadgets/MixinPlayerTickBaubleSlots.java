package org.fentanylsolutions.thaumicdabblery.mixins.late.witchinggadgets;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleSlotsFeature;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.WitchingBaubleSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import baubles.api.BaublesApi;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mixin(targets = "witchinggadgets.common.util.handler.PlayerTickHandler", remap = false)
public abstract class MixinPlayerTickBaubleSlots {

    @Redirect(
        method = "playerTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/oredict/OreDictionary;itemMatches(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Z)Z"))
    private boolean td$sniperRing(ItemStack expected, ItemStack actual, boolean strict,
        TickEvent.PlayerTickEvent event) {
        // Resolve the actual ring, including forks whose hardcoded metadata is wrong.
        return BaubleSlotsFeature.isEnabled() ? WitchingBaubleSlots.sniper(BaublesApi.getBaubles(event.player)) != null
            : OreDictionary.itemMatches(expected, actual, strict);
    }
}
