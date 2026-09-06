package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.feature.wandcomponents.WandComponentStatsFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.lib.crafting.ArcaneSceptreRecipe;
import thaumcraft.common.lib.crafting.ArcaneWandRecipe;

/** Guard the ingredients being assembled, not every theoretically possible registered combination. */
@Mixin(value = { ArcaneWandRecipe.class, ArcaneSceptreRecipe.class }, remap = false)
public abstract class MixinArcaneWandRecipeCost {

    @Shadow(remap = false)
    private boolean checkItemEquals(ItemStack target, ItemStack input) {
        throw new AssertionError();
    }

    @Inject(method = "matches", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$safeMatch(IInventory inventory, World world, EntityPlayer player,
        CallbackInfoReturnable<Boolean> cir) {
        if (thaumicdabblery$unsafeCost(inventory)) cir.setReturnValue(false);
    }

    @Inject(method = "getCraftingResult", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$safeOutput(IInventory inventory, CallbackInfoReturnable<ItemStack> cir) {
        if (thaumicdabblery$unsafeCost(inventory)) cir.setReturnValue(null);
    }

    @Inject(
        method = "getAspects(Lnet/minecraft/inventory/IInventory;)Lthaumcraft/api/aspects/AspectList;",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private void thaumicdabblery$safeAspects(IInventory inventory, CallbackInfoReturnable<AspectList> cir) {
        if (thaumicdabblery$unsafeCost(inventory)) cir.setReturnValue(new AspectList());
    }

    @Unique
    private boolean thaumicdabblery$unsafeCost(IInventory inventory) {
        if (!WandComponentStatsFeature.isEnabled()) return false;
        boolean sceptre = (Object) this instanceof ArcaneSceptreRecipe;
        ItemStack cap = ThaumcraftApiHelper.getStackInRowAndColumn(inventory, sceptre ? 1 : 0, sceptre ? 0 : 2);
        ItemStack core = ThaumcraftApiHelper.getStackInRowAndColumn(inventory, 1, 1);
        if (cap == null || core == null) return false;

        WandCap matchedCap = null;
        for (WandCap candidate : WandCap.caps.values()) {
            // Use each recipe's own matching rules, including its NBT/wildcard behavior and registry order.
            if (checkItemEquals(cap, candidate.getItem())) {
                matchedCap = candidate;
                break;
            }
        }
        if (matchedCap == null) return false;
        for (WandRod candidate : WandRod.rods.values()) {
            if (checkItemEquals(core, candidate.getItem())) {
                int capCost = matchedCap.getCraftCost();
                int coreCost = candidate.getCraftCost();
                if (capCost < 0 || coreCost < 0) return true;
                long cost = (long) capCost * coreCost;
                // Check before TC's int multiplication or ItemStack's signed-short serialization can overflow.
                // Short-circuit before scaling, since even the long product could otherwise overflow.
                return cost > Short.MAX_VALUE || sceptre && cost * 3 / 2 > Short.MAX_VALUE;
            }
        }
        return false;
    }
}
