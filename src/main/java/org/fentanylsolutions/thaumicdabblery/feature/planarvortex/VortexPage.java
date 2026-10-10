package org.fentanylsolutions.thaumicdabblery.feature.planarvortex;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.kentington.thaumichorizons.common.ThaumicHorizons;
import com.kentington.thaumichorizons.common.items.ItemWandCastingDisposable;

import thaumcraft.api.research.ResearchPage;
import thaumcraft.common.config.ConfigItems;

/** A book page references a recipe, rather than maintaining a second recipe definition. */
public final class VortexPage extends ResearchPage {

    public final String key;
    public final ItemStack outputIcon;

    public VortexPage(String key, ItemStack outputIcon) {
        super("");
        this.key = key;
        this.outputIcon = outputIcon == null ? null : outputIcon.copy();
        refresh();
    }

    public VortexRecipes.Recipe resolve() {
        return resolve(key);
    }

    public void refresh() {
        VortexRecipes.Recipe current = resolve();
        // Display icons for creatures must never masquerade as a craftable item in TC's recipe lookup.
        recipeOutput = current == null || current.output == null ? null : current.output.copy();
    }

    public static VortexRecipes.Recipe resolve(String key) {
        if (key == null || VortexRecipes.DISABLED.contains(key)) return null;
        VortexRecipes.Recipe custom = VortexRecipes.RECIPES.get(key);
        if (custom != null) return custom;
        ItemStack input, output = null;
        String entity = null;
        switch (key) {
            case "builtin:void_putty":
                input = new ItemStack(ConfigItems.itemResource, 1, 16);
                output = new ItemStack(ThaumicHorizons.itemVoidPutty);
                break;
            case "builtin:crystal_wand":
                input = new ItemStack(ThaumicHorizons.itemCrystalWand);
                output = ((ItemWandCastingDisposable) ThaumicHorizons.itemWandCastingDisposable).wand.copy();
                break;
            case "builtin:wisps":
                input = new ItemStack(ConfigItems.itemResource, 1, 14);
                entity = "Thaumcraft.Wisp";
                break;
            case "builtin:void_golem":
                input = new ItemStack(ThaumicHorizons.itemGolemPowder);
                entity = "ThaumicHorizons.GolemTH";
                break;
            default:
                return null;
        }
        return new VortexRecipes.Recipe(input, output, entity, new NBTTagCompound());
    }
}
