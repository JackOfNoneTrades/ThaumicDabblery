package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;
import org.fentanylsolutions.thaumicdabblery.feature.witcherybranch.VirtualItemUseState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.infusion.Infusion;
import com.emoniph.witchery.infusion.infusions.symbols.EffectRegistry;
import com.emoniph.witchery.infusion.infusions.symbols.SymbolEffect;
import com.emoniph.witchery.item.ItemMysticBranch;

@Mixin(value = ItemMysticBranch.class, remap = false)
public abstract class MixinItemMysticBranch {

    @Unique
    private Map<EntityPlayer, Integer> thaumicdabblery$lastHeartbeat;

    @Inject(method = "onItemRightClick", at = @At("RETURN"))
    private void thaumicdabblery$logVirtualUseStarted(ItemStack stack, World world, EntityPlayer player,
        CallbackInfoReturnable<ItemStack> cir) {
        if (VirtualItemUseState.isActive(player)) {
            thaumicdabblery$logState("Witchery onItemRightClick returned", player, stack, -1);
        }
    }

    @Inject(method = "onUsingTick", at = @At("HEAD"))
    private void thaumicdabblery$logVirtualUsingTick(ItemStack stack, EntityPlayer player, int count, CallbackInfo ci) {
        if (!ThaumicDabblery.isDebugMode() || !VirtualItemUseState.isActive(player)) {
            return;
        }
        if (thaumicdabblery$lastHeartbeat == null) {
            thaumicdabblery$lastHeartbeat = new WeakHashMap<>();
        }
        Integer previous = thaumicdabblery$lastHeartbeat.get(player);
        if (previous == null || previous - count >= 20) {
            thaumicdabblery$lastHeartbeat.put(player, count);
            thaumicdabblery$logState("Witchery onUsingTick reached", player, stack, count);
        }
    }

    @Inject(method = "addNewStroke", at = @At("RETURN"))
    private void thaumicdabblery$logStroke(NBTTagCompound data, byte[] previousStrokes, byte stroke,
        CallbackInfoReturnable<byte[]> cir) {
        if (!ThaumicDabblery.isDebugMode()) {
            return;
        }
        byte[] strokes = cir.getReturnValue();
        ThaumicDabblery.debug(
            "[Mystic Branch/Witchery] Added stroke " + stroke
                + "; sequence="
                + Arrays.toString(strokes)
                + "; registry="
                + thaumicdabblery$describeEffect(strokes));
    }

    @Inject(method = "onPlayerStoppedUsing", at = @At("HEAD"))
    private void thaumicdabblery$logVirtualUseStopping(ItemStack stack, World world, EntityPlayer player,
        int remainingUseCount, CallbackInfo ci) {
        if (VirtualItemUseState.isActive(player)) {
            thaumicdabblery$logState("Witchery onPlayerStoppedUsing entered", player, stack, remainingUseCount);
        }
    }

    @Inject(method = "onPlayerStoppedUsing", at = @At("RETURN"))
    private void thaumicdabblery$logVirtualUseStopped(ItemStack stack, World world, EntityPlayer player,
        int remainingUseCount, CallbackInfo ci) {
        if (VirtualItemUseState.isActive(player)) {
            thaumicdabblery$logState("Witchery onPlayerStoppedUsing returned", player, stack, remainingUseCount);
            if (thaumicdabblery$lastHeartbeat != null) {
                thaumicdabblery$lastHeartbeat.remove(player);
            }
        }
    }

    @Unique
    private static void thaumicdabblery$logState(String stage, EntityPlayer player, ItemStack stack, int useCount) {
        if (!ThaumicDabblery.isDebugMode()) {
            return;
        }
        NBTTagCompound data = player.getEntityData();
        byte[] strokes = data.getByteArray("Strokes");
        NBTTagCompound infusion = Infusion.getNBT(player);
        ThaumicDabblery.debug(
            "[Mystic Branch/Witchery] " + stage
                + " on "
                + (player.worldObj.isRemote ? "client" : "server")
                + ": player="
                + player.getCommandSenderName()
                + ", stack="
                + (stack == null ? "null" : stack.getUnlocalizedName() + ":" + stack.getItemDamage())
                + ", useCount="
                + useCount
                + ", strokes="
                + Arrays.toString(strokes)
                + ", registry="
                + thaumicdabblery$describeEffect(strokes)
                + ", preparedEffect="
                + (data.hasKey("WITCSpellEffectID") ? data.getInteger("WITCSpellEffectID") : "missing")
                + ", enhancedLevel="
                + (data.hasKey("WITCSpellEffectEnhanced") ? data.getInteger("WITCSpellEffectEnhanced") : "missing")
                + ", witcheryInfusion="
                + (infusion != null && infusion.hasKey("witcheryInfusionID") ? infusion.getInteger("witcheryInfusionID")
                    : "missing")
                + ", charges="
                + (infusion != null && infusion.hasKey("witcheryInfusionCharges")
                    ? infusion.getInteger("witcheryInfusionCharges")
                    : "missing"));
    }

    @Unique
    private static String thaumicdabblery$describeEffect(byte[] strokes) {
        EffectRegistry registry = EffectRegistry.instance();
        SymbolEffect effect = registry.getEffect(strokes);
        return effect == null ? "none (complete=" + registry.contains(strokes) + ")"
            : "effectID=" + effect.getEffectID() + ", level=" + registry.getLevel(strokes);
    }
}
