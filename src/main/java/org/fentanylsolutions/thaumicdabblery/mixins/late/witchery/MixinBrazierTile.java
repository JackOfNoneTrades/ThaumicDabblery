package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.feature.witcherybrazier.BrazierRegistry;
import org.fentanylsolutions.thaumicdabblery.feature.witcherybrazier.WitcheryBrazierFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.emoniph.witchery.blocks.BlockBrazier.TileEntityBrazier;
import com.emoniph.witchery.crafting.BrazierRecipes;
import com.emoniph.witchery.crafting.BrazierRecipes.BrazierRecipe;

@Mixin(value = TileEntityBrazier.class, remap = false)
public abstract class MixinBrazierTile {

    @Shadow
    private ItemStack[] slots;
    @Shadow
    private int furnaceCookTime;
    @Shadow
    private long storage;
    @Unique
    private BrazierRecipe td$active;
    @Unique
    private NBTTagCompound td$signature;

    @Inject(method = "updateEntity", at = @At("HEAD"), remap = true, require = 1)
    private void td$changed(CallbackInfo ci) {
        if (!WitcheryBrazierFeature.isEnabled() || ((TileEntityBrazier) (Object) this).getWorldObj().isRemote) return;
        BrazierRecipe active = slots[3] == null ? null
            : BrazierRecipes.instance()
                .getRecipe(new ItemStack[] { slots[0], slots[1], slots[2] });
        if (active != td$active || td$signature != null && active == null) {
            if (active != null && BrazierRegistry.definition(active) == null) BrazierRegistry.recipes();
            NBTTagCompound signature = BrazierRegistry.signature(active);
            if (!Objects.equals(signature, td$signature)) {
                furnaceCookTime = 0;
                storage = 0;
            }
            td$signature = signature;
            td$active = active;
        } else if (active == null) {
            furnaceCookTime = 0;
            storage = 0;
        }
    }

    @ModifyArg(
        method = { "updateEntity", "func_145845_h" },
        at = @At(value = "INVOKE", target = "Lcom/emoniph/witchery/common/IPowerSource;consumePower(F)Z"),
        index = 0,
        require = 1)
    private float td$cost(float cost) {
        BrazierRegistry.Recipe r = BrazierRegistry.definition(td$active);
        return WitcheryBrazierFeature.isEnabled() && r != null ? r.power : cost;
    }

    @Redirect(
        method = { "updateEntity", "func_145845_h" },
        at = @At(
            value = "INVOKE",
            target = "Lcom/emoniph/witchery/crafting/BrazierRecipes$BrazierRecipe;onBurnt(Lnet/minecraft/world/World;IIIJLcom/emoniph/witchery/blocks/BlockBrazier$TileEntityBrazier;)V"),
        require = 1)
    private void td$finish(BrazierRecipe recipe, World world, int x, int y, int z, long ticks, TileEntityBrazier tile) {
        try {
            recipe.onBurnt(world, x, y, z, ticks, tile);
        } finally {
            if (WitcheryBrazierFeature.isEnabled()) storage = 0;
        }
    }

    @Inject(method = "reset", at = @At("HEAD"), require = 1)
    private void td$reset(CallbackInfo ci) {
        if (!WitcheryBrazierFeature.isEnabled()) return;
        furnaceCookTime = 0;
        storage = 0;
        td$active = null;
        td$signature = null;
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"), remap = true, require = 1)
    private void td$write(NBTTagCompound tag, CallbackInfo ci) {
        tag.setInteger("CookTime", furnaceCookTime);
        if (td$signature != null) tag.setTag("ThaumicDabbleryBrazierRecipe", td$signature.copy());
        else tag.removeTag("ThaumicDabbleryBrazierRecipe");
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"), remap = true, require = 1)
    private void td$read(NBTTagCompound tag, CallbackInfo ci) {
        furnaceCookTime = tag.getInteger("CookTime");
        td$active = null;
        td$signature = tag.hasKey("ThaumicDabbleryBrazierRecipe", 10)
            ? tag.getCompoundTag("ThaumicDabbleryBrazierRecipe")
            : null;
    }
}
