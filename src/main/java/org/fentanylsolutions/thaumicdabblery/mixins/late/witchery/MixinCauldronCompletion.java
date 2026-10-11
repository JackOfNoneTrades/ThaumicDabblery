package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.feature.witcherycauldron.CauldronRecipes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.brewing.RitualStatus;
import com.emoniph.witchery.brewing.WitcheryBrewRegistry;
import com.emoniph.witchery.util.EntityUtil;

@Mixin(value = WitcheryBrewRegistry.class, remap = false)
public abstract class MixinCauldronCompletion {

    // Pending crafts deliberately finish even after the script or integration has been disabled.
    @Inject(method = "updateRitual", at = @At("HEAD"), cancellable = true, require = 1)
    private void td$finish(MinecraftServer server, World world, int x, int y, int z, NBTTagCompound brew, int coven,
        int pulses, boolean leonard, CallbackInfoReturnable<RitualStatus> cir) {
        if (brew == null || !brew.hasKey(CauldronRecipes.PENDING)) return;
        cir.setReturnValue(RitualStatus.FAILED);
        if (world.isRemote) return;
        NBTTagCompound pending = brew.getCompoundTag(CauldronRecipes.PENDING);
        if (pending.getBoolean("Completed")) return;
        ItemStack result = ItemStack.loadItemStackFromNBT(pending.getCompoundTag("Output"));
        if (result == null) return;
        EntityItem entity = new EntityItem(world, x + 0.5, y + 1.5, z + 0.5, result);
        entity.motionX = entity.motionZ = 0;
        entity.motionY = 0.2;
        EntityUtil.spawnEntityInWorld(world, entity);
        pending.setBoolean("Completed", true);
        cir.setReturnValue(RitualStatus.COMPLETE);
    }
}
