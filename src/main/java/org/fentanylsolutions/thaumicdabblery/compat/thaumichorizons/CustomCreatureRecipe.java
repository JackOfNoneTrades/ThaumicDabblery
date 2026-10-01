package org.fentanylsolutions.thaumicdabblery.compat.thaumichorizons;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.INpc;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

import com.kentington.thaumichorizons.common.ThaumicHorizons;
import com.kentington.thaumichorizons.common.lib.CreatureInfusionRecipe;
import com.kentington.thaumichorizons.common.lib.EntityInfusionProperties;
import com.kentington.thaumichorizons.common.tiles.TileVat;

import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.entities.golems.EntityGolemBase;

/** Runtime support has no scripting dependencies: saved transformations can finish without MineTweaker. */
public final class CustomCreatureRecipe extends CreatureInfusionRecipe {

    public static final String OUTPUT_LABEL = "thaumicdabblery:creature";
    private final String outputEntity;

    public CustomCreatureRecipe(String research, Class<? extends EntityLiving> input, String output, int instability,
        AspectList aspects, ItemStack[] components) {
        super(research, null, instability, aspects, input, components, 0);
        outputEntity = output;
    }

    public String getOutputEntity() {
        return outputEntity;
    }

    @Override
    public Object getRecipeOutput(Class input) {
        NBTTagCompound output = new NBTTagCompound();
        output.setString("entity", outputEntity);
        return new Object[] { OUTPUT_LABEL, output };
    }

    @Override
    public boolean matches(ArrayList<ItemStack> input, Class central, World world, EntityPlayer player) {
        return central == getRecipeInput() && super.matches(input, central, world, player);
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends EntityLiving> requireMob(String id, boolean input) {
        Class<?> type = (Class<?>) EntityList.stringToClassMapping.get(id);
        if (type == null || !EntityLiving.class.isAssignableFrom(type) || Modifier.isAbstract(type.getModifiers()))
            throw new IllegalArgumentException("Unknown or unsupported creature entity: " + id);
        if (input && isExcludedInput(type))
            throw new IllegalArgumentException("Horizons forbids this vat input: " + id);
        try {
            type.getConstructor(World.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Entity has no standard World constructor: " + id, e);
        }
        return (Class<? extends EntityLiving>) type;
    }

    private static boolean isExcludedInput(Class<?> type) {
        if (EntityGolem.class.isAssignableFrom(type) || EntityGolemBase.class.isAssignableFrom(type)
            || IMerchant.class.isAssignableFrom(type)
            || INpc.class.isAssignableFrom(type)) return true;
        for (Class<?> banned : ThaumicHorizons.classBanList) if (banned.isAssignableFrom(type)) return true;
        return false;
    }

    public static boolean acceptsUndead(EntityLivingBase entity) {
        if (!(entity instanceof EntityLiving) || entity.getCreatureAttribute() != EnumCreatureAttribute.UNDEAD
            || isExcludedInput(entity.getClass())) return false;
        for (CreatureInfusionRecipe recipe : ThaumicHorizons.critterRecipes)
            if (recipe instanceof CustomCreatureRecipe && recipe.getRecipeInput() == entity.getClass()) return true;
        return false;
    }

    public static CreatureInfusionRecipe findRecipe(EntityLivingBase entity, ArrayList<ItemStack> components,
        EntityPlayer player) {
        if (entity.getCreatureAttribute() != EnumCreatureAttribute.UNDEAD)
            return ThaumicHorizons.getCreatureInfusion(entity, components, player);
        // Opening the vat to an explicitly scripted undead input must not enable native upgrades on undead mobs.
        if (!acceptsUndead(entity)) return null;
        for (CreatureInfusionRecipe recipe : ThaumicHorizons.critterRecipes) if (recipe instanceof CustomCreatureRecipe
            && recipe.matches(components, entity.getClass(), player.worldObj, player)) return recipe;
        return null;
    }

    public static void finish(TileVat vat, NBTTagCompound output, AspectList cost) {
        EntityLivingBase source = vat.getEntityContained();
        String id = output.getString("entity");
        try {
            requireMob(id, false);
            EntityLiving result = (EntityLiving) EntityList.createEntityByName(id, vat.getWorldObj());
            if (result == null || source == null) throw new IllegalStateException("Missing source or output creature");
            // Copy only the name. A full NBT copy would replace the new species' attributes with the source's.
            if (source instanceof EntityLiving && ((EntityLiving) source).hasCustomNameTag())
                result.setCustomNameTag(((EntityLiving) source).getCustomNameTag());
            ((EntityInfusionProperties) result.getExtendedProperties("CreatureInfusion")).addCost(cost);
            result.func_110163_bv();
            vat.setEntityContained(result);
        } catch (RuntimeException failure) {
            ThaumicDabblery.LOG
                .error("Could not finish creature transformation to {}; original creature retained", id, failure);
        }
    }
}
