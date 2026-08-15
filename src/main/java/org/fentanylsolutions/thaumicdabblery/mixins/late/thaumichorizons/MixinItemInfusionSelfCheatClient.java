package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumichorizons;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

import org.fentanylsolutions.thaumicdabblery.feature.witcherybranch.WitcheryBranchFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.Witchery;
import com.kentington.thaumichorizons.common.items.ItemInfusionSelfCheat;

@Mixin(value = ItemInfusionSelfCheat.class, remap = false)
public abstract class MixinItemInfusionSelfCheatClient {

    @Inject(method = "getSubItems", at = @At("TAIL"))
    private void thaumicdabblery$addMysticBranchSelfItem(Item item, CreativeTabs tab, List<ItemStack> items,
        CallbackInfo ci) {
        if (WitcheryBranchFeature.isActive()) {
            items.add(new ItemStack(item, 1, WitcheryBranchFeature.INFUSION_ID));
        }
    }

    @Inject(method = "getIconFromDamage", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$useMysticBranchIcon(int damage, CallbackInfoReturnable<IIcon> cir) {
        if (damage == WitcheryBranchFeature.INFUSION_ID) {
            cir.setReturnValue(Witchery.Items.MYSTIC_BRANCH.getIconFromDamage(0));
        }
    }

    @Inject(method = "getItemStackDisplayName", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$nameMysticBranchSelfItem(ItemStack stack, CallbackInfoReturnable<String> cir) {
        if (stack.getItemDamage() == WitcheryBranchFeature.INFUSION_ID) {
            cir.setReturnValue(StatCollector.translateToLocal("selfInfusions.thaumicdabbleryMysticBranch"));
        }
    }
}
