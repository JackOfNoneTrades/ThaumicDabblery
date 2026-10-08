package org.fentanylsolutions.thaumicdabblery.compat.thaumichorizons;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.INpc;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
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
    private static final Map<CreatureInfusionRecipe, Set<Class<?>>> BLACKLIST = new IdentityHashMap<>();
    private final String outputEntity;
    private final NBTTagCompound inputNBT, outputNBT;
    private final float breachPower;

    public interface BreachVat {

        void thaumicdabblery$dismantleForBreach();
    }

    public static void setBlacklist(CreatureInfusionRecipe recipe, Set<Class<?>> types) {
        if (recipe == null) return;
        if (types.isEmpty()) BLACKLIST.remove(recipe);
        else BLACKLIST.put(recipe, new HashSet<>(types));
    }

    public CustomCreatureRecipe(String research, Class<? extends EntityLiving> input, String output, int instability,
        AspectList aspects, ItemStack[] components) {
        this(research, input, output, instability, aspects, components, new NBTTagCompound(), new NBTTagCompound(), 0);
    }

    private CustomCreatureRecipe(String research, Class<? extends EntityLiving> input, String output, int instability,
        AspectList aspects, ItemStack[] components, NBTTagCompound inputNBT, NBTTagCompound outputNBT,
        float breachPower) {
        super(research, null, instability, aspects, input, components, 0);
        outputEntity = output;
        this.inputNBT = (NBTTagCompound) inputNBT.copy();
        this.outputNBT = (NBTTagCompound) outputNBT.copy();
        this.breachPower = breachPower;
    }

    public CustomCreatureRecipe withRecipe(String research, int instability, AspectList aspects,
        ItemStack[] components) {
        return new CustomCreatureRecipe(
            research,
            getRecipeInput(),
            outputEntity,
            instability,
            aspects,
            components,
            inputNBT,
            outputNBT,
            breachPower);
    }

    public CustomCreatureRecipe withNBT(NBTTagCompound input, NBTTagCompound output) {
        for (String key : new String[] { "id", "Pos", "Motion", "Rotation", "Dimension", "UUIDMost", "UUIDLeast",
            "Riding" })
            if (output.hasKey(key)) throw new IllegalArgumentException("Output entity NBT cannot override " + key);
        return new CustomCreatureRecipe(
            getResearch(),
            getRecipeInput(),
            outputEntity,
            getInstability(),
            getAspects(),
            getComponents(),
            input,
            output,
            breachPower);
    }

    public CustomCreatureRecipe withBreach(float power) {
        if (Float.isNaN(power) || Float.isInfinite(power) || power < 0 || power > 32)
            throw new IllegalArgumentException("Breach explosion strength must be between 0 and 32 (0 disables it)");
        return new CustomCreatureRecipe(
            getResearch(),
            getRecipeInput(),
            outputEntity,
            getInstability(),
            getAspects(),
            getComponents(),
            inputNBT,
            outputNBT,
            power);
    }

    public String getOutputEntity() {
        return outputEntity;
    }

    @Override
    public Object getRecipeOutput(Class input) {
        NBTTagCompound output = new NBTTagCompound();
        output.setString("entity", outputEntity);
        output.setTag("nbt", outputNBT.copy());
        output.setFloat("breach", breachPower);
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
        if (entity == null) return null;
        boolean undead = entity.getCreatureAttribute() == EnumCreatureAttribute.UNDEAD;
        // Opening the vat to scripted undead must not enable native upgrades on undead mobs.
        if (undead && !acceptsUndead(entity)) return null;
        NBTTagCompound actual = null;
        for (CreatureInfusionRecipe recipe : ThaumicHorizons.critterRecipes) {
            if (undead && !(recipe instanceof CustomCreatureRecipe)) continue;
            Set<Class<?>> blocked = BLACKLIST.get(recipe);
            if (blocked != null && blocked.contains(entity.getClass())) continue;
            if (!recipe.matches(components, entity.getClass(), player.worldObj, player)) continue;
            if (recipe instanceof CustomCreatureRecipe && !((CustomCreatureRecipe) recipe).inputNBT.hasNoTags()) {
                if (actual == null) {
                    actual = new NBTTagCompound();
                    entity.writeToNBT(actual);
                }
                if (!containsNBT(actual, ((CustomCreatureRecipe) recipe).inputNBT)) continue;
            }
            return recipe;
        }
        return null;
    }

    private static boolean containsNBT(NBTTagCompound actual, NBTTagCompound required) {
        for (Object key : required.func_150296_c()) {
            String name = (String) key;
            NBTBase expected = required.getTag(name), found = actual.getTag(name);
            if (expected instanceof NBTTagCompound) {
                if (!(found instanceof NBTTagCompound)
                    || !containsNBT((NBTTagCompound) found, (NBTTagCompound) expected)) return false;
            } else if (!expected.equals(found)) return false;
        }
        return true;
    }

    private static void mergeNBT(NBTTagCompound target, NBTTagCompound patch) {
        for (Object key : patch.func_150296_c()) {
            String name = (String) key;
            NBTBase value = patch.getTag(name);
            if (value instanceof NBTTagCompound && target.getTag(name) instanceof NBTTagCompound)
                mergeNBT(target.getCompoundTag(name), (NBTTagCompound) value);
            else target.setTag(name, value.copy());
        }
    }

    /** True means the vat was dismantled and its normal completion packet must not reference the removed tile. */
    public static boolean finish(TileVat vat, NBTTagCompound output, AspectList cost) {
        EntityLivingBase source = vat.getEntityContained();
        String id = output.getString("entity");
        EntityLiving result;
        float power = output.getFloat("breach");
        try {
            requireMob(id, false);
            if (Float.isNaN(power) || Float.isInfinite(power) || power < 0 || power > 32)
                throw new IllegalArgumentException("Invalid saved breach strength");
            result = (EntityLiving) EntityList.createEntityByName(id, vat.getWorldObj());
            if (result == null || source == null) throw new IllegalStateException("Missing source or output creature");
            // Copy only the name. A full source NBT copy would replace the new species' attributes.
            if (source instanceof EntityLiving && ((EntityLiving) source).hasCustomNameTag())
                result.setCustomNameTag(((EntityLiving) source).getCustomNameTag());
            NBTTagCompound patch = output.getCompoundTag("nbt");
            if (!patch.hasNoTags()) {
                NBTTagCompound data = new NBTTagCompound();
                result.writeToNBT(data);
                mergeNBT(data, patch);
                result.readFromNBT(data);
            }
            ((EntityInfusionProperties) result.getExtendedProperties("CreatureInfusion")).addCost(cost);
            result.func_110163_bv();
        } catch (RuntimeException failure) {
            ThaumicDabblery.LOG
                .error("Could not finish creature transformation to {}; original creature retained", id, failure);
            return false;
        }
        if (power == 0) {
            vat.setEntityContained(result);
            return false;
        }
        World world = vat.getWorldObj();
        int x = vat.xCoord, y = vat.yCoord, z = vat.zCoord;
        // Detach first: native disassembly must not kill the subject and release flux.
        vat.setEntityContained(null);
        vat.mode = 0;
        // Remove the matrix while its controller still exists; its break callback expects a TileVat below it.
        boolean dropMatrix = world.getBlock(x, y + 1, z) == ThaumicHorizons.blockModifiedMatrix;
        int matrixMetadata = world.getBlockMetadata(x, y + 1, z);
        if (dropMatrix) world.func_147480_a(x, y + 1, z, false);
        ((BreachVat) vat).thaumicdabblery$dismantleForBreach();
        world.playSoundEffect(x + 0.5, y - 1.5, z + 0.5, "dig.glass", 1.0F, 1.0F);
        world.setBlockToAir(x, y - 1, z);
        world.setBlockToAir(x, y - 2, z);
        world.playSoundEffect(x + 0.5, y - 1.5, z + 0.5, "liquid.water", 1.0F, 1.0F);
        world.createExplosion(null, x + 0.5, y - 1.5, z + 0.5, power, true);
        // Normal block drops, delayed until after the blast so the matrix item survives.
        if (dropMatrix) ThaumicHorizons.blockModifiedMatrix.dropBlockAsItem(world, x, y + 1, z, matrixMetadata, 0);
        result.setLocationAndAngles(x + 0.5, y - 2, z + 0.5, 0, 0);
        if (!world.spawnEntityInWorld(result))
            ThaumicDabblery.LOG.error("Creature breach output {} could not spawn after vat disassembly", id);
        return true;
    }
}
