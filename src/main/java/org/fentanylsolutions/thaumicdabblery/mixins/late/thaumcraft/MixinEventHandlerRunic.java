package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import java.util.Arrays;
import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;

import org.fentanylsolutions.thaumicdabblery.feature.itemstats.RunicShieldingRegistry;
import org.fentanylsolutions.thaumicdabblery.feature.itemstats.ThaumcraftItemStatsFeature;
import org.fentanylsolutions.thaumicdabblery.feature.itemstats.WarpingGearRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import baubles.api.BaublesApi;
import thaumcraft.api.IRunicArmor;
import thaumcraft.common.items.baubles.ItemAmuletRunic;
import thaumcraft.common.items.baubles.ItemGirdleRunic;
import thaumcraft.common.items.baubles.ItemRingRunic;
import thaumcraft.common.lib.events.EventHandlerRunic;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketRunicCharge;

@Mixin(value = EventHandlerRunic.class, remap = false)
public abstract class MixinEventHandlerRunic {

    private static final String HARDENING_TAG = "RS.HARDEN";
    private static final int THAUMCRAFT_BAUBLE_SLOTS = 4;

    @Shadow
    public HashMap<Integer, Integer> runicCharge;

    @Shadow
    public HashMap<Integer, Integer[]> runicInfo;

    @Inject(method = "getFinalWarp", at = @At("HEAD"), cancellable = true, require = 1)
    private static void thaumicdabblery$getScriptedWarp(ItemStack stack, EntityPlayer player,
        CallbackInfoReturnable<Integer> cir) {
        if (!ThaumcraftItemStatsFeature.isEnabled()) {
            return;
        }

        Integer scriptedWarp = WarpingGearRegistry.get(stack);
        if (scriptedWarp != null) {
            cir.setReturnValue(scriptedWarp);
        }
    }

    @Inject(method = "getFinalCharge", at = @At("HEAD"), cancellable = true, require = 1)
    private static void thaumicdabblery$getScriptedRunicCharge(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (!ThaumcraftItemStatsFeature.isEnabled() || stack == null || stack.getItem() == null) {
            return;
        }

        Integer scriptedBase = RunicShieldingRegistry.getBase(stack);
        if (scriptedBase != null) {
            cir.setReturnValue(clampCharge((long) scriptedBase + getHardeningTag(stack)));
        } else if (RunicShieldingRegistry.isAugmentable(stack) && !(stack.getItem() instanceof IRunicArmor)) {
            cir.setReturnValue(clampCharge(getHardeningTag(stack)));
        }
    }

    @Inject(method = "getHardening", at = @At("HEAD"), cancellable = true, require = 1)
    private static void thaumicdabblery$getScriptedHardening(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (ThaumcraftItemStatsFeature.isEnabled() && RunicShieldingRegistry.isAugmentable(stack)
            && !(stack.getItem() instanceof IRunicArmor)) {
            cir.setReturnValue(getHardeningTag(stack));
        }
    }

    @Inject(method = "livingTick", at = @At("TAIL"), require = 1)
    private void thaumicdabblery$includeScriptedRunicItems(LivingEvent.LivingUpdateEvent event, CallbackInfo ci) {
        if (!ThaumcraftItemStatsFeature.isEnabled() || event.entity.worldObj.isRemote
            || !(event.entity instanceof EntityPlayerMP)) {
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) event.entity;
        IInventory baubles = BaublesApi.getBaubles(player);
        if (!hasScriptedRunicItem(player, baubles)) {
            return;
        }

        int max = 0;
        int charged = 0;
        int kinetic = 0;
        int healing = 0;
        int emergency = 0;

        for (int slot = 0; slot < 4; slot++) {
            ItemStack stack = player.inventory.armorItemInSlot(slot);
            if (isRunic(stack)) {
                max = saturatingAdd(max, EventHandlerRunic.getFinalCharge(stack));
            }
        }

        for (int slot = 0; slot < Math.min(THAUMCRAFT_BAUBLE_SLOTS, baubles.getSizeInventory()); slot++) {
            ItemStack stack = baubles.getStackInSlot(slot);
            if (!isRunic(stack)) {
                continue;
            }

            max = saturatingAdd(max, EventHandlerRunic.getFinalCharge(stack));
            if (stack.getItem() instanceof ItemRingRunic) {
                if (stack.getItemDamage() == 2) {
                    charged++;
                } else if (stack.getItemDamage() == 3) {
                    healing++;
                }
            } else if (stack.getItem() instanceof ItemAmuletRunic && stack.getItemDamage() == 1) {
                emergency++;
            } else if (stack.getItem() instanceof ItemGirdleRunic && stack.getItemDamage() == 1) {
                kinetic++;
            }
        }

        updateRunicState(player, max, charged, kinetic, healing, emergency);
    }

    private void updateRunicState(EntityPlayerMP player, int max, int charged, int kinetic, int healing,
        int emergency) {
        int id = player.getEntityId();
        Integer[] previousInfo = runicInfo.get(id);
        int previousCharge = runicCharge.containsKey(id) ? runicCharge.get(id) : 0;
        int charge = Math.max(0, Math.min(previousCharge, max));

        if (max > 0) {
            Integer[] newInfo = { max, charged, kinetic, healing, emergency };
            runicInfo.put(id, newInfo);
            runicCharge.put(id, charge);
            if (!Arrays.equals(previousInfo, newInfo) || previousCharge != charge) {
                PacketHandler.INSTANCE.sendTo(new PacketRunicCharge(player, (short) charge, max), player);
            }
        } else {
            runicInfo.remove(id);
            runicCharge.put(id, 0);
            if (previousInfo != null || previousCharge != 0) {
                PacketHandler.INSTANCE.sendTo(new PacketRunicCharge(player, (short) 0, 0), player);
            }
        }
    }

    private static boolean hasScriptedRunicItem(EntityPlayer player, IInventory baubles) {
        for (int slot = 0; slot < 4; slot++) {
            if (RunicShieldingRegistry.isAugmentable(player.inventory.armorItemInSlot(slot))) {
                return true;
            }
        }
        for (int slot = 0; slot < Math.min(THAUMCRAFT_BAUBLE_SLOTS, baubles.getSizeInventory()); slot++) {
            if (RunicShieldingRegistry.isAugmentable(baubles.getStackInSlot(slot))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRunic(ItemStack stack) {
        return stack != null && (stack.getItem() instanceof IRunicArmor || RunicShieldingRegistry.isAugmentable(stack));
    }

    private static int getHardeningTag(ItemStack stack) {
        return stack.hasTagCompound() && stack.stackTagCompound.hasKey(HARDENING_TAG)
            ? stack.stackTagCompound.getByte(HARDENING_TAG)
            : 0;
    }

    private static int clampCharge(long charge) {
        return charge <= 0 ? 0 : charge >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) charge;
    }

    private static int saturatingAdd(int left, int right) {
        return clampCharge((long) left + right);
    }
}
