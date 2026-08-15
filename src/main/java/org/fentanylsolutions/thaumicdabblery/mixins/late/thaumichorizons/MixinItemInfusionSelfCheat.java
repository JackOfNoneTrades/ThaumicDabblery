package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumichorizons;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;
import org.fentanylsolutions.thaumicdabblery.feature.witcherybranch.WitcheryBranchFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.kentington.thaumichorizons.common.ThaumicHorizons;
import com.kentington.thaumichorizons.common.items.ItemInfusionSelfCheat;
import com.kentington.thaumichorizons.common.lib.EntityInfusionProperties;

@Mixin(value = ItemInfusionSelfCheat.class, remap = false)
public abstract class MixinItemInfusionSelfCheat {

    @Inject(method = { "onItemRightClick", "func_77659_a" }, at = @At("RETURN"), remap = false)
    private void thaumicdabblery$grantMysticBranchSelfInfusion(ItemStack stack, World world, EntityPlayer player,
        CallbackInfoReturnable<ItemStack> cir) {
        if (!WitcheryBranchFeature.isActive() || stack.getItemDamage() != WitcheryBranchFeature.INFUSION_ID) {
            return;
        }

        ensureSelfInfusion(player);
    }

    private static void ensureSelfInfusion(EntityPlayer player) {
        EntityInfusionProperties properties = (EntityInfusionProperties) player
            .getExtendedProperties(EntityInfusionProperties.EXT_PROP_NAME);
        if (properties == null || properties.hasPlayerInfusion(WitcheryBranchFeature.INFUSION_ID)) {
            return;
        }

        properties.addPlayerInfusion(WitcheryBranchFeature.INFUSION_ID);
        ThaumicHorizons.instance.eventHandlerEntity.applyInfusions(player);
        ThaumicDabblery
            .debug("[Mystic Branch/creative self item] Granted self-infusion to " + player.getCommandSenderName());
    }
}
