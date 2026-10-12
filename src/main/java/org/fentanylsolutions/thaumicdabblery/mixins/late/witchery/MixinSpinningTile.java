package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.fentanylsolutions.thaumicdabblery.feature.witcheryspinningwheel.SpinningRegistry;
import org.fentanylsolutions.thaumicdabblery.feature.witcheryspinningwheel.WitcherySpinningWheelFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.blocks.BlockSpinningWheel.TileEntitySpinningWheel;
import com.emoniph.witchery.crafting.SpinningRecipes.SpinningRecipe;

@Mixin(value = TileEntitySpinningWheel.class, remap = false)
public abstract class MixinSpinningTile {

    @Shadow
    private ItemStack[] slots;
    @Shadow
    public int furnaceCookTime;

    @Unique
    private SpinningRecipe td$activeRecipe() {
        return com.emoniph.witchery.crafting.SpinningRecipes.instance()
            .getRecipe(slots[0], new ItemStack[] { slots[1], slots[3], slots[4] });
    }

    @Unique
    private NBTTagCompound td$recipe;
    @Unique
    private SpinningRecipe td$last;

    @Inject(method = "canSmelt", at = @At("HEAD"), cancellable = true, require = 1)
    private void td$capacity(CallbackInfoReturnable<Boolean> cir) {
        if (!WitcherySpinningWheelFeature.isEnabled()) return;
        SpinningRecipe recipe = td$activeRecipe();
        if (recipe == null) {
            cir.setReturnValue(false);
            return;
        }
        ItemStack out = recipe.result, current = slots[2];
        if (out.stackSize > Math.min(64, out.getMaxStackSize())
            || current != null && (!current.isItemEqual(out) || !ItemStack.areItemStackTagsEqual(current, out)
                || current.stackSize + out.stackSize > Math.min(64, current.getMaxStackSize()))) {
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "updateEntity", at = @At("HEAD"), remap = true, require = 1)
    private void td$recipeChange(CallbackInfo ci) {
        if (!WitcherySpinningWheelFeature.isEnabled()
            || ((TileEntitySpinningWheel) (Object) this).getWorldObj().isRemote) return;
        SpinningRecipe recipe = td$activeRecipe();
        if (recipe != td$last || td$recipe != null && recipe == null) {
            NBTTagCompound signature = SpinningRegistry.signature(recipe);
            if (!Objects.equals(td$recipe, signature)) furnaceCookTime = 0;
            td$recipe = signature;
            td$last = recipe;
        } else if (recipe == null) furnaceCookTime = 0;
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"), remap = true, require = 1)
    private void td$write(NBTTagCompound tag, CallbackInfo ci) {
        if (td$recipe != null) tag.setTag("ThaumicDabblerySpinningRecipe", td$recipe.copy());
        else tag.removeTag("ThaumicDabblerySpinningRecipe");
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"), remap = true, require = 1)
    private void td$read(NBTTagCompound tag, CallbackInfo ci) {
        td$last = null;
        td$recipe = tag.hasKey("ThaumicDabblerySpinningRecipe", 10)
            ? tag.getCompoundTag("ThaumicDabblerySpinningRecipe")
            : null;
    }

    @Inject(method = { "isItemValidForSlot", "func_94041_b" }, at = @At("HEAD"), cancellable = true, require = 1)
    private void td$outputSlots(int slot, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        if (WitcherySpinningWheelFeature.isEnabled() && slot == 2) cir.setReturnValue(false);
    }
}
