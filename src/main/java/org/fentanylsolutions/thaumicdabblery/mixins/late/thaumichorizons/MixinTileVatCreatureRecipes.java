package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumichorizons;

import java.util.ArrayList;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.fentanylsolutions.thaumicdabblery.compat.thaumichorizons.CustomCreatureRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.kentington.thaumichorizons.common.lib.CreatureInfusionRecipe;
import com.kentington.thaumichorizons.common.lib.networking.PacketFXInfusionDone;
import com.kentington.thaumichorizons.common.lib.networking.PacketHandler;
import com.kentington.thaumichorizons.common.tiles.TileVat;

import cpw.mods.fml.common.network.NetworkRegistry;
import thaumcraft.api.aspects.AspectList;

@Mixin(value = TileVat.class, remap = false)
public abstract class MixinTileVatCreatureRecipes implements CustomCreatureRecipe.BreachVat {

    @Unique
    private boolean thaumicdabblery$breaching, thaumicdabblery$dismantling;

    @Override
    public void thaumicdabblery$dismantleForBreach() {
        thaumicdabblery$breaching = true;
        try {
            ((TileVat) (Object) this).killMe();
        } finally {
            thaumicdabblery$breaching = thaumicdabblery$dismantling = false;
        }
    }

    @Inject(method = "killMe", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$disassembleOnce(CallbackInfo ci) {
        if (!thaumicdabblery$breaching) return;
        if (thaumicdabblery$dismantling) ci.cancel();
        else thaumicdabblery$dismantling = true;
    }

    @Shadow
    public int recipeType;
    @Shadow
    public int mode;
    @Shadow
    private AspectList myEssentia;

    @Shadow
    private String recipeOutputLabel;

    @Inject(method = "startInfusion", at = @At("HEAD"), require = 1)
    private void thaumicdabblery$resetOutputLabel(EntityPlayer player, CallbackInfo ci) {
        recipeOutputLabel = "";
    }

    @Inject(method = "isValidInfusionTarget", at = @At("RETURN"), cancellable = true, require = 1)
    private void thaumicdabblery$allowScriptedUndead(CallbackInfoReturnable<Boolean> ci) {
        if (!ci.getReturnValue() && CustomCreatureRecipe.acceptsUndead(((TileVat) (Object) this).getEntityContained()))
            ci.setReturnValue(true);
    }

    @Redirect(
        method = "startInfusion",
        at = @At(
            value = "INVOKE",
            target = "Lcom/kentington/thaumichorizons/common/ThaumicHorizons;getCreatureInfusion(Lnet/minecraft/entity/EntityLivingBase;Ljava/util/ArrayList;Lnet/minecraft/entity/player/EntityPlayer;)Lcom/kentington/thaumichorizons/common/lib/CreatureInfusionRecipe;"),
        require = 1)
    private CreatureInfusionRecipe thaumicdabblery$selectRecipe(EntityLivingBase entity,
        ArrayList<ItemStack> components, EntityPlayer player) {
        return CustomCreatureRecipe.findRecipe(entity, components, player);
    }

    @Inject(method = "craftingFinish", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$finishTransformation(Object output, String label, CallbackInfo ci) {
        if (recipeType != 0 || !CustomCreatureRecipe.OUTPUT_LABEL.equals(label) || !(output instanceof NBTTagCompound))
            return;
        TileVat vat = (TileVat) (Object) this;
        boolean breached = CustomCreatureRecipe.finish(vat, (NBTTagCompound) output, myEssentia);
        mode = 0;
        if (breached) {
            ci.cancel();
            return;
        }
        PacketHandler.INSTANCE.sendToAllAround(
            new PacketFXInfusionDone(vat.xCoord, vat.yCoord - 1, vat.zCoord),
            new NetworkRegistry.TargetPoint(
                vat.getWorldObj().provider.dimensionId,
                vat.xCoord,
                vat.yCoord,
                vat.zCoord,
                32.0));
        vat.getWorldObj()
            .markBlockForUpdate(vat.xCoord, vat.yCoord, vat.zCoord);
        vat.markDirty();
        ci.cancel();
    }
}
