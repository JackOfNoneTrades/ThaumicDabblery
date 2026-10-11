package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidTank;

import org.fentanylsolutions.thaumicdabblery.feature.witcherycauldron.CauldronRecipes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.brewing.TileEntityCauldron;
import com.emoniph.witchery.brewing.action.BrewAction;

@Mixin(value = TileEntityCauldron.class, remap = false)
public abstract class MixinCauldronCapture {

    @Shadow
    private FluidTank tank;
    @Unique
    private CauldronRecipes.Recipe td$selected;

    @Inject(method = "addItem", at = @At("HEAD"), require = 1)
    private void td$select(BrewAction action, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        td$selected = tank.getFluid() == null ? null : CauldronRecipes.selectBeforeAdd(action, tank.getFluid().tag);
    }

    @Inject(method = "addItem", at = @At("RETURN"), require = 1)
    private void td$capture(BrewAction action, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        CauldronRecipes.Recipe recipe = td$selected;
        td$selected = null;
        if (!cir.getReturnValue() || recipe == null || tank.getFluid() == null || tank.getFluid().tag == null) return;
        NBTTagCompound brew = tank.getFluid().tag;
        if (!brew.getBoolean("RitualTriggered")) return;
        NBTTagCompound pending = new NBTTagCompound();
        pending.setString("Id", recipe.id);
        pending.setTag("Output", recipe.output.writeToNBT(new NBTTagCompound()));
        if (recipe.power != null) brew.setInteger("Power", recipe.power);
        pending.setInteger("Power", brew.getInteger("Power"));
        brew.setTag(CauldronRecipes.PENDING, pending);
        ((TileEntityCauldron) (Object) this).markBlockForUpdate(true);
    }
}
