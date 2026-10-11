package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.feature.witcherykettle.KettleBatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.blocks.BlockKettle;
import com.emoniph.witchery.crafting.KettleRecipes;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

@Mixin(value = BlockKettle.class, remap = false)
public abstract class MixinKettleCollection {

    // Capture before decrStackSize resets the kettle on collection of its last item.
    @Inject(method = "onBlockActivated", at = @At("HEAD"), remap = true, require = 1)
    private void td$begin(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz,
        CallbackInfoReturnable<Boolean> cir, @Share("td$batch") LocalRef<KettleBatch> batch) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof KettleBatch.Holder) batch.set(((KettleBatch.Holder) tile).td$batch());
    }

    @Redirect(
        method = "onBlockActivated",
        remap = true,
        at = @At(
            value = "INVOKE",
            target = "Lcom/emoniph/witchery/crafting/KettleRecipes;isBrewableBy(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;)Z",
            remap = false),
        require = 1)
    private boolean td$familiar(KettleRecipes registry, ItemStack stack, EntityPlayer player,
        @Share("td$batch") LocalRef<KettleBatch> batch) {
        return batch.get() != null ? batch.get()
            .canCollect(player) : registry.isBrewableBy(stack, player);
    }

    @Redirect(
        method = "onBlockActivated",
        remap = true,
        at = @At(
            value = "INVOKE",
            target = "Lcom/emoniph/witchery/crafting/KettleRecipes;getHatBonus(Lnet/minecraft/item/ItemStack;)I",
            remap = false),
        require = 2)
    private int td$bonus(KettleRecipes registry, ItemStack stack, @Share("td$batch") LocalRef<KettleBatch> batch) {
        return batch.get() != null ? batch.get().bonus : registry.getHatBonus(stack);
    }
}
