package org.fentanylsolutions.thaumicdabblery.feature.witcherybrazier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;
import org.fentanylsolutions.thaumicdabblery.feature.witcheryspinningwheel.SpinningRegistry;
import org.fentanylsolutions.thaumicdabblery.mixins.late.witchery.BrazierRecipeAccessor;

import com.emoniph.witchery.crafting.BrazierRecipes;
import com.emoniph.witchery.crafting.BrazierRecipes.BrazierRecipe;
import com.emoniph.witchery.util.ParticleEffect;
import com.emoniph.witchery.util.SoundEffect;

public final class BrazierRegistry {

    private static final Map<BrazierRecipe, Recipe> DEFINITIONS = new IdentityHashMap<>();

    private BrazierRegistry() {}

    public static Recipe definition(BrazierRecipe k) {
        return DEFINITIONS.get(k);
    }

    public static List<Recipe> recipes() {
        List<Recipe> result = new ArrayList<>();
        for (BrazierRecipe k : BrazierRecipes.instance().recipes) {
            Recipe r = DEFINITIONS.get(k);
            if (r == null) {
                String id = "builtin:" + k.unlocalizedName.replace("witchery.brazier.", "");
                for (Recipe existing : DEFINITIONS.values()) if (existing.id.equals(id))
                    throw new IllegalArgumentException("duplicate built-in brazier name: " + id);
                r = new Recipe(id, k);
                DEFINITIONS.put(k, r);
            }
            result.add(r);
        }
        return Collections.unmodifiableList(result);
    }

    public static void replace(List<Recipe> rules) {
        for (Recipe r : rules) if (r.changed) validate(r);
        List<BrazierRecipe> next = new ArrayList<>(), previous = new ArrayList<>(BrazierRecipes.instance().recipes);
        Map<BrazierRecipe, Recipe> definitions = new IdentityHashMap<>();
        try {
            for (Recipe r : rules) {
                // The native constructor registers itself. Restore the list until the entire update is ready.
                BrazierRecipe k = r.changed ? BrazierRecipeAccessor.td$create(
                    r.effect == null ? r.id : r.effect.unlocalizedName,
                    r.inBook,
                    r.burnTicks,
                    SpinningRegistry.copies(r.items)) : r.effect;
                next.add(k);
                definitions.put(k, r);
            }
        } finally {
            BrazierRecipes.instance().recipes.clear();
            BrazierRecipes.instance().recipes.addAll(previous);
        }
        BrazierRecipes.instance().recipes.clear();
        BrazierRecipes.instance().recipes.addAll(next);
        DEFINITIONS.clear();
        DEFINITIONS.putAll(definitions);
    }

    public static ItemStack input(ItemStack s) {
        s = SpinningRegistry.stack(s, true, true);
        if (s.getItem() == Items.flint_and_steel || s.getItem() == Items.water_bucket
            || s.getItem() == Items.potionitem && s.getItemDamage() == 0)
            throw new IllegalArgumentException(
                "flint and steel, water buckets and water bottles control the brazier and cannot be ingredients");
        return s;
    }

    public static void requireEntity(String name) {
        Object type = EntityList.stringToClassMapping.get(name);
        if (!(type instanceof Class) || !EntityLiving.class.isAssignableFrom((Class<?>) type))
            throw new IllegalArgumentException("unknown living mob: " + name);
    }

    public static NBTTagCompound entityData(NBTTagCompound tag) {
        for (String key : new String[] { "id", "Pos", "Motion", "Rotation", "Dimension", "UUIDMost", "UUIDLeast",
            "Riding" }) if (tag.hasKey(key)) throw new IllegalArgumentException("entity NBT cannot override " + key);
        return (NBTTagCompound) tag.copy();
    }

    private static void validate(Recipe r) {
        if (r.items == null || r.items.length != 3)
            throw new IllegalArgumentException("a brazier recipe needs exactly three ingredients");
        for (ItemStack s : r.items) input(s);
        if (r.burnTicks < 1) throw new IllegalArgumentException("burn ticks must be positive");
        if (!Float.isFinite(r.power) || r.power < 0)
            throw new IllegalArgumentException("power per tick must be finite and nonnegative");
        if (r.entity != null) {
            requireEntity(r.entity);
            entityData(r.nbt);
        }
        if (r.effect == null && r.entity == null) throw new IllegalArgumentException("recipe has no effect");
        if (r.title != null && (r.title.trim()
            .isEmpty() || r.description == null))
            throw new IllegalArgumentException("book title must not be empty and description cannot be null");
    }

    public static NBTTagCompound signature(BrazierRecipe k) {
        if (k == null) return null;
        Recipe r = definition(k);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Effect", r != null && r.effect != null ? r.effect.unlocalizedName : k.unlocalizedName);
        if (r != null) tag.setString("ID", r.id);
        tag.setInteger("Ticks", k.burnTicks);
        tag.setFloat("Power", r != null ? r.power : k.getNeedsPower() ? 1 : 0);
        NBTTagList items = new NBTTagList();
        for (ItemStack s : k.items) items.appendTag(s.writeToNBT(new NBTTagCompound()));
        tag.setTag("Items", items);
        if (r != null && r.entity != null) {
            tag.setString("Entity", r.entity);
            tag.setTag("EntityData", r.nbt.copy());
        }
        return tag;
    }

    public static String bookText(Recipe r, String nativeText) {
        StringBuilder text = new StringBuilder();
        if (r.title == null) text.append(nativeText);
        else {
            text.append("\u00a7n")
                .append(r.title)
                .append("\u00a7r\n\n")
                .append(r.description)
                .append("\n\n");
            for (ItemStack s : r.items) text.append("\u00a78>\u00a70 ")
                .append(s.getDisplayName())
                .append('\n');
        }
        text.append("\nBase burn time: ")
            .append(r.burnTicks / 20.0)
            .append(" seconds\nAltar drain: ")
            .append(r.power)
            .append(" per working tick");
        return text.toString();
    }

    public static void summon(Recipe r, World world, int x, int y, int z) {
        try {
            EntityLiving mob = (EntityLiving) EntityList.createEntityByName(r.entity, world);
            if (mob == null) throw new IllegalStateException("entity factory returned null");
            mob.setLocationAndAngles(x + 0.5, y + 1, z + 0.5, world.rand.nextFloat() * 360, 0);
            mob.onSpawnWithEgg(null);
            NBTTagCompound data = new NBTTagCompound();
            mob.writeToNBT(data);
            merge(data, r.nbt);
            mob.readFromNBT(data);
            // Search nearby space using the real mob bounds, including large or flying creatures.
            for (int radius = 0; radius <= 3; radius++) for (int dy = 1; dy <= 3; dy++)
                for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    mob.setLocationAndAngles(x + dx + 0.5, y + dy, z + dz + 0.5, mob.rotationYaw, 0);
                    if (!world.getCollidingBoundingBoxes(mob, mob.boundingBox)
                        .isEmpty() || !world.checkNoEntityCollision(mob.boundingBox)) continue;
                    if (!world.spawnEntityInWorld(mob)) throw new IllegalStateException("entity spawn was rejected");
                    ParticleEffect.INSTANT_SPELL
                        .send(SoundEffect.NOTE_HARP, world, mob.posX, mob.posY, mob.posZ, 0.5, 1.0, 16);
                    return;
                }
            throw new IllegalStateException("no free space near the brazier");
        } catch (RuntimeException exception) {
            ThaumicDabblery.LOG
                .error("Brazier recipe {} could not summon {} at {},{},{}", r.id, r.entity, x, y, z, exception);
        }
    }

    private static void merge(NBTTagCompound target, NBTTagCompound patch) {
        for (Object name : patch.func_150296_c()) {
            String key = (String) name;
            NBTBase value = patch.getTag(key);
            if (value instanceof NBTTagCompound && target.getTag(key) instanceof NBTTagCompound)
                merge(target.getCompoundTag(key), (NBTTagCompound) value);
            else target.setTag(key, value.copy());
        }
    }

    public static final class Recipe {

        public final String id;
        public final BrazierRecipe effect;
        public boolean changed, inBook;
        public ItemStack[] items;
        public int burnTicks;
        public float power;
        public String title, description, entity;
        public NBTTagCompound nbt = new NBTTagCompound();

        private Recipe(String id, BrazierRecipe k) {
            this.id = id;
            effect = k;
            items = SpinningRegistry.copies(k.items);
            burnTicks = k.burnTicks;
            power = k.getNeedsPower() ? 1 : 0;
            inBook = k.inBook;
        }

        public Recipe(String id, ItemStack[] items, String entity, int ticks, NBTTagCompound nbt) {
            this.id = id;
            this.items = items;
            this.entity = entity;
            burnTicks = ticks;
            this.nbt = entityData(nbt);
            effect = null;
            power = 1;
            inBook = true;
            changed = true;
            title = "Summon " + entity;
            description = "Summons a creature when the offerings finish burning.";
        }

        private Recipe(String id, Recipe r) {
            this.id = id;
            effect = r.effect;
            items = SpinningRegistry.copies(r.items);
            burnTicks = r.burnTicks;
            power = r.power;
            inBook = r.inBook;
            title = r.title;
            description = r.description;
            entity = r.entity;
            nbt = (NBTTagCompound) r.nbt.copy();
            changed = true;
        }

        public Recipe copy(String id) {
            return new Recipe(id, this);
        }

        public Recipe copy() {
            return copy(id);
        }
    }
}
