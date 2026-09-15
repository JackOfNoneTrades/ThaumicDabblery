package org.fentanylsolutions.thaumicdabblery.mixins.late.witchinggadgets.legacy;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleSlotsFeature;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.LegacyWitchingBaubles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import baubles.api.BaubleType;
import baubles.api.IBauble;

/** Native cloaks have only Traveller's Gear callbacks; kamas already override these IBauble methods. */
@Mixin(targets = "witchinggadgets.common.items.baubles.ItemCloak", remap = false)
public abstract class MixinItemCloakBauble implements IBauble {

    @Shadow
    public abstract void onItemTicked(EntityPlayer player, ItemStack stack);

    @Shadow
    public abstract void onItemEquipped(EntityPlayer player, ItemStack stack);

    @Shadow
    public abstract void onItemUnequipped(EntityPlayer player, ItemStack stack);

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return null;
    }

    @Override
    public boolean canEquip(ItemStack stack, EntityLivingBase player) {
        return true;
    }

    @Override
    public boolean canUnequip(ItemStack stack, EntityLivingBase player) {
        return true;
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) onItemTicked((EntityPlayer) player, stack);
    }

    @Override
    public void onEquipped(ItemStack stack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) onItemEquipped((EntityPlayer) player, stack);
    }

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) onItemUnequipped((EntityPlayer) player, stack);
    }

    @Redirect(
        method = "activate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/EntityPlayer;openGui(Ljava/lang/Object;ILnet/minecraft/world/World;III)V"))
    private void td$openExactStorage(EntityPlayer player, Object mod, int gui, World world, int x, int y, int z,
        EntityPlayer activatingPlayer, ItemStack stack) {
        int slot = BaubleSlotsFeature.isEnabled() ? LegacyWitchingBaubles.slot(player, stack) : -1;
        player.openGui(mod, gui, world, slot >= 0 ? slot : x, slot >= 0 ? LegacyWitchingBaubles.GUI_MARKER : y, z);
    }
}
