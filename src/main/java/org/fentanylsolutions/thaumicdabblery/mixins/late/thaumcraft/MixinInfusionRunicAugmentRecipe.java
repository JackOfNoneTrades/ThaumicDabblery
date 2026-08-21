package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.feature.itemstats.RunicShieldingRegistry;
import org.fentanylsolutions.thaumicdabblery.feature.itemstats.ThaumcraftItemStatsFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import thaumcraft.api.IRunicArmor;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.lib.crafting.InfusionRunicAugmentRecipe;

@Mixin(value = InfusionRunicAugmentRecipe.class, remap = false)
public abstract class MixinInfusionRunicAugmentRecipe {

    @Inject(method = "matches", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$matchScriptedRunicItem(ArrayList<ItemStack> input, ItemStack central, World world,
        EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (!ThaumcraftItemStatsFeature.isEnabled() || central == null
            || central.getItem() == null
            || central.getItem() instanceof IRunicArmor
            || !RunicShieldingRegistry.isAugmentable(central)) {
            return;
        }

        InfusionRunicAugmentRecipe recipe = (InfusionRunicAugmentRecipe) (Object) this;
        String research = recipe.getResearch();
        if (research.length() > 0 && !ThaumcraftApiHelper.isResearchComplete(player.getCommandSenderName(), research)) {
            cir.setReturnValue(false);
            return;
        }

        ArrayList<ItemStack> remaining = new ArrayList<>();
        for (ItemStack stack : input) {
            remaining.add(stack.copy());
        }

        for (ItemStack component : recipe.getComponents(central)) {
            boolean found = false;
            for (int index = 0; index < remaining.size(); index++) {
                ItemStack candidate = remaining.get(index)
                    .copy();
                if (component.getItemDamage() == Short.MAX_VALUE) {
                    candidate.setItemDamage(Short.MAX_VALUE);
                }
                if (InfusionRecipe.areItemStacksEqual(candidate, component, true)) {
                    remaining.remove(index);
                    found = true;
                    break;
                }
            }
            if (!found) {
                cir.setReturnValue(false);
                return;
            }
        }

        cir.setReturnValue(remaining.isEmpty());
    }
}
