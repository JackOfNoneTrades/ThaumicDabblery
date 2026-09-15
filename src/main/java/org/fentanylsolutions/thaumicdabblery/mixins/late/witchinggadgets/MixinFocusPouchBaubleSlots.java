package org.fentanylsolutions.thaumicdabblery.mixins.late.witchinggadgets;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleSlotsFeature;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.WitchingBaubleSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import baubles.api.BaublesApi;
import thaumcraft.common.container.ContainerFocusPouch;
import thaumcraft.common.container.InventoryFocusPouch;
import thaumcraft.common.items.wands.ItemFocusPouch;

@Pseudo
@Mixin(
    targets = { "witchinggadgets.common.pouch.ContainerPatchedFocusPouch",
        "witchinggadgets.asm.pouch.ContainerPatchedFocusPouch" },
    remap = false)
public abstract class MixinFocusPouchBaubleSlots {

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 4))
    private int td$allSlots(int original, InventoryPlayer inv, World world, int x, int y, int z) {
        return BaubleSlotsFeature.isEnabled() ? BaublesApi.getBaubles(inv.player)
            .getSizeInventory() : original;
    }

    @Inject(method = { "onContainerClosed", "func_75134_a" }, at = @At("HEAD"), cancellable = true)
    private void td$saveActualPouch(EntityPlayer player, CallbackInfo ci) {
        if (!BaubleSlotsFeature.isEnabled()) return;
        ci.cancel();
        if (player.worldObj.isRemote) return;
        ItemStack cursor = player.inventory.getItemStack();
        if (cursor != null) {
            player.dropPlayerItemWithRandomChoice(cursor, false);
            player.inventory.setItemStack(null);
        }
        ItemStack pouch = ((FocusPouchAccessor) this).td$getPouch();
        if (pouch != null && pouch.getItem() instanceof ItemFocusPouch) {
            ((ItemFocusPouch) pouch.getItem())
                .setInventory(pouch, ((InventoryFocusPouch) ((ContainerFocusPouch) (Object) this).input).stackList);
            WitchingBaubleSlots.sync(player, pouch);
        }
    }
}
