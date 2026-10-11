package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.fentanylsolutions.thaumicdabblery.feature.witcherykettle.KettleBatch;
import org.fentanylsolutions.thaumicdabblery.feature.witcherykettle.WitcheryKettleFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.emoniph.witchery.blocks.BlockKettle.TileEntityKettle;
import com.emoniph.witchery.crafting.KettleRecipes;
import com.emoniph.witchery.crafting.KettleRecipes.KettleRecipe;

@Mixin(value = TileEntityKettle.class, remap = false)
public abstract class MixinKettleTile implements KettleBatch.Holder {

    @Unique
    private KettleBatch td$batch;
    @Shadow
    private ItemStack[] furnaceItemStacks;

    public KettleBatch td$batch() {
        return td$batch;
    }

    @Redirect(
        method = "updateEntity",
        remap = true,
        at = @At(
            value = "INVOKE",
            target = "Lcom/emoniph/witchery/crafting/KettleRecipes$KettleRecipe;getOutput(Lnet/minecraft/entity/player/EntityPlayer;Z)Lnet/minecraft/item/ItemStack;",
            remap = false),
        require = 1)
    private ItemStack td$capture(KettleRecipe recipe, EntityPlayer player, boolean copy) {
        td$batch = WitcheryKettleFeature.isEnabled() ? new KettleBatch(recipe) : null;
        return recipe.getOutput(player, copy);
    }

    @Redirect(
        method = "canExtractItem",
        remap = true,
        at = @At(
            value = "INVOKE",
            target = "Lcom/emoniph/witchery/crafting/KettleRecipes;isBrewableBy(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;)Z",
            remap = false),
        require = 1)
    private boolean td$automation(KettleRecipes registry, ItemStack stack, EntityPlayer player) {
        return td$batch != null ? td$batch.canCollect(player) : registry.isBrewableBy(stack, player);
    }

    @Inject(method = "reset", at = @At("HEAD"), require = 1)
    private void td$clear(boolean flush, CallbackInfo ci) {
        td$batch = null;
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"), remap = true, require = 1)
    private void td$write(NBTTagCompound tag, CallbackInfo ci) {
        if (td$batch != null && furnaceItemStacks[6] != null) tag.setTag(KettleBatch.TAG, td$batch.write());
        else tag.removeTag(KettleBatch.TAG);
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"), remap = true, require = 1)
    private void td$read(NBTTagCompound tag, CallbackInfo ci) {
        td$batch = furnaceItemStacks[6] != null && tag.hasKey(KettleBatch.TAG, 10)
            ? new KettleBatch(tag.getCompoundTag(KettleBatch.TAG))
            : null;
    }
}
