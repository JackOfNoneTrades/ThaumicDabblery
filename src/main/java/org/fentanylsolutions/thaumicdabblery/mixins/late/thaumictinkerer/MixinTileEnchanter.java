package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumictinkerer;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.fentanylsolutions.thaumicdabblery.feature.osmotic.OsmoticRecipes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumic.tinkerer.common.block.tile.TileEnchanter;

@Mixin(value = TileEnchanter.class, remap = false)
public abstract class MixinTileEnchanter extends TileEntity {

    @Unique
    private boolean thaumicdabblery$retrySelection;

    @Unique
    private void thaumicdabblery$removed(boolean wand) {
        TileEnchanter tile = (TileEnchanter) (Object) this;
        if (tile.working && worldObj != null && !worldObj.isRemote) {
            thaumicdabblery$retrySelection = wand;
            OsmoticRecipes.cancel(tile, wand);
        }
    }

    @Inject(method = { "decrStackSize", "func_70298_a" }, at = @At("HEAD"), remap = false, require = 1)
    private void thaumicdabblery$extract(int slot, int amount, CallbackInfoReturnable<ItemStack> ci) {
        if (amount <= 0) return;
        if ((slot == 0 || slot == 1) && ((TileEnchanter) (Object) this).getStackInSlot(slot) != null)
            thaumicdabblery$removed(slot == 1);
        if (slot == 0) thaumicdabblery$retrySelection = false;
    }

    @Inject(method = { "setInventorySlotContents", "func_70299_a" }, at = @At("HEAD"), remap = false, require = 1)
    private void thaumicdabblery$replace(int slot, ItemStack stack, CallbackInfo ci) {
        if (stack == ((TileEnchanter) (Object) this).getStackInSlot(slot)) return;
        if (slot == 0 || slot == 1) thaumicdabblery$removed(slot == 1);
        if (slot == 0) thaumicdabblery$retrySelection = false;
    }

    @Inject(method = "markDirty", at = @At("HEAD"), remap = true, cancellable = true, require = 1)
    private void thaumicdabblery$keepRetrySelection(CallbackInfo ci) {
        if (!thaumicdabblery$retrySelection) return;
        super.markDirty();
        OsmoticRecipes.sync((TileEnchanter) (Object) this);
        ci.cancel();
    }

    @Inject(method = "readCustomNBT", at = @At("TAIL"), require = 1)
    private void thaumicdabblery$readRetry(NBTTagCompound tag, CallbackInfo ci) {
        thaumicdabblery$retrySelection = tag.getBoolean("tdOsmoticRetrySelection");
    }

    @Inject(method = "writeCustomNBT", at = @At("TAIL"), require = 1)
    private void thaumicdabblery$writeRetry(NBTTagCompound tag, CallbackInfo ci) {
        tag.setBoolean("tdOsmoticRetrySelection", thaumicdabblery$retrySelection);
    }

    @Inject(method = "updateEntity", at = @At("HEAD"), remap = true, require = 1)
    private void thaumicdabblery$refresh(CallbackInfo ci) {
        TileEnchanter tile = (TileEnchanter) (Object) this;
        if (tile.working && tile.getStackInSlot(0) == null) thaumicdabblery$removed(false);
        else if (tile.working && !OsmoticRecipes.validWand(tile.getStackInSlot(1))) thaumicdabblery$removed(true);
        if (OsmoticRecipes.refresh(tile)) OsmoticRecipes.sync(tile);
    }

    @Redirect(
        method = "updateEntity",
        remap = true,
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/common/items/wands/ItemWandCasting;getAllVis(Lnet/minecraft/item/ItemStack;)Lthaumcraft/api/aspects/AspectList;",
            remap = false),
        require = 1)
    private AspectList thaumicdabblery$discountedGate(ItemWandCasting item, ItemStack wand) {
        return OsmoticRecipes.payableAspects(wand);
    }

    @Inject(method = "updateAspectList", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$costs(CallbackInfo ci) {
        OsmoticRecipes.refresh((TileEnchanter) (Object) this);
        ci.cancel();
    }
}
