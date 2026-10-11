package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.fentanylsolutions.thaumicdabblery.feature.witcherydistillery.DistilleryRegistry;
import org.fentanylsolutions.thaumicdabblery.feature.witcherydistillery.WitcheryDistilleryFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.blocks.BlockDistillery.TileEntityDistillery;
import com.emoniph.witchery.crafting.DistilleryRecipes.DistilleryRecipe;

@Mixin(value = TileEntityDistillery.class, remap = false)
public abstract class MixinDistilleryTile {

    @Shadow
    private ItemStack[] furnaceItemStacks;
    @Shadow
    public int furnaceCookTime;

    @Shadow
    public abstract DistilleryRecipe getActiveRecipe();

    @Unique
    private NBTTagCompound td$recipe;
    @Unique
    private DistilleryRecipe td$last;

    @Inject(method = "canSmelt", at = @At("HEAD"), cancellable = true, require = 1)
    private void td$capacity(CallbackInfoReturnable<Boolean> cir) {
        if (!WitcheryDistilleryFeature.isEnabled()) return;
        DistilleryRecipe recipe = getActiveRecipe();
        if (recipe == null) {
            cir.setReturnValue(false);
            return;
        }
        for (int i = 0; i < recipe.outputs.length; i++) {
            ItemStack out = recipe.outputs[i], current = furnaceItemStacks[i + 3];
            if (out == null) continue;
            if (out.stackSize > Math.min(64, out.getMaxStackSize())
                || current != null && (!current.isItemEqual(out) || !ItemStack.areItemStackTagsEqual(current, out)
                    || current.stackSize + out.stackSize > Math.min(64, current.getMaxStackSize()))) {
                cir.setReturnValue(false);
                return;
            }
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "updateEntity", at = @At("HEAD"), remap = true, require = 1)
    private void td$recipeChange(CallbackInfo ci) {
        if (!WitcheryDistilleryFeature.isEnabled() || ((TileEntityDistillery) (Object) this).getWorldObj().isRemote)
            return;
        DistilleryRecipe recipe = getActiveRecipe();
        if (recipe != td$last || td$recipe != null && recipe == null) {
            NBTTagCompound signature = DistilleryRegistry.signature(recipe);
            if (!Objects.equals(td$recipe, signature)) furnaceCookTime = 0;
            td$recipe = signature;
            td$last = recipe;
        } else if (recipe == null) furnaceCookTime = 0;
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"), remap = true, require = 1)
    private void td$write(NBTTagCompound tag, CallbackInfo ci) {
        if (td$recipe != null) tag.setTag("ThaumicDabbleryDistilleryRecipe", td$recipe.copy());
        else tag.removeTag("ThaumicDabbleryDistilleryRecipe");
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"), remap = true, require = 1)
    private void td$read(NBTTagCompound tag, CallbackInfo ci) {
        td$last = null;
        td$recipe = tag.hasKey("ThaumicDabbleryDistilleryRecipe", 10)
            ? tag.getCompoundTag("ThaumicDabbleryDistilleryRecipe")
            : null;
    }

    @Inject(method = { "isItemValidForSlot", "func_94041_b" }, at = @At("HEAD"), cancellable = true, require = 1)
    private void td$outputSlots(int slot, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        if (WitcheryDistilleryFeature.isEnabled() && slot >= 3) cir.setReturnValue(false);
    }
}
