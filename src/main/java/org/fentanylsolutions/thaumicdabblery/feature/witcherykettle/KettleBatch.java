package org.fentanylsolutions.thaumicdabblery.feature.witcherykettle;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import org.fentanylsolutions.thaumicdabblery.mixins.late.witchery.KettleRecipeAccessor;

import com.emoniph.witchery.crafting.KettleRecipes.KettleRecipe;
import com.emoniph.witchery.familiar.Familiar;

public final class KettleBatch {

    public static final String TAG = "ThaumicDabbleryKettleBatch";
    public final int familiar, bonus;

    public KettleBatch(KettleRecipe recipe) {
        KettleRecipeAccessor a = (KettleRecipeAccessor) recipe;
        familiar = a.td$familiar();
        bonus = a.td$bonus();
    }

    public KettleBatch(NBTTagCompound tag) {
        familiar = tag.getInteger("Familiar");
        bonus = tag.getInteger("Bonus");
    }

    public boolean canCollect(EntityPlayer player) {
        return familiar == 0 || player != null && Familiar.getActiveFamiliarType(player) == familiar;
    }

    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Familiar", familiar);
        tag.setInteger("Bonus", bonus);
        return tag;
    }

    public interface Holder {

        KettleBatch td$batch();
    }
}
