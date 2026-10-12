package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import org.fentanylsolutions.thaumicdabblery.feature.witcherybrazier.BrazierRegistry;
import org.fentanylsolutions.thaumicdabblery.feature.witcherybrazier.BrazierRegistry.Recipe;
import org.fentanylsolutions.thaumicdabblery.feature.witcherybrazier.WitcheryBrazierFeature;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;
import minetweaker.api.data.IData;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.mc1710.data.NBTConverter;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.witchery.Brazier")
public final class WitcheryBrazierZen {

    private WitcheryBrazierZen() {}

    public static void init() {
        MineTweakerAPI.registerClass(WitcheryBrazierZen.class);
    }

    public static void registerCommand() {
        if (MineTweakerAPI.server != null) MineTweakerAPI.server.addMineTweakerCommand(
            "witcheryBrazier",
            new String[] { "/mt witcheryBrazier",
                "    Lists brazier recipe IDs, ingredients, effects, burn times and power costs in minetweaker.log" },
            (args, player) -> {
                dump();
                if (player != null) player.sendChat(
                    MineTweakerImplementationAPI.platform.getMessage("Brazier recipes written to minetweaker.log"));
            });
    }

    private static ItemStack[] items(IItemStack[] values) {
        if (values == null || values.length != 3)
            throw new IllegalArgumentException("expected exactly three ingredients");
        ItemStack[] result = new ItemStack[3];
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null || values[i].hasTransformers())
                throw new IllegalArgumentException("ingredients cannot be null or have transformations");
            result[i] = BrazierRegistry.input(MineTweakerMC.getItemStack(values[i]));
        }
        return result;
    }

    private static void newId(List<Recipe> rules, String id) {
        if (id == null || !id.matches("custom:[A-Za-z0-9_./-]+"))
            throw new IllegalArgumentException("recipe ID must start with custom:");
        for (Recipe r : rules) if (id.equals(r.id)) throw new IllegalArgumentException("duplicate recipe ID: " + id);
    }

    private static int find(List<Recipe> rules, String id) {
        for (int i = 0; i < rules.size(); i++) if (id != null && id.equals(rules.get(i).id)) return i;
        throw new IllegalArgumentException("unknown brazier recipe: " + id);
    }

    @ZenMethod
    public static void copyRecipe(String id, String source) {
        change("copy " + source + " to " + id, rules -> {
            newId(rules, id);
            rules.add(
                0,
                rules.get(find(rules, source))
                    .copy(id));
        });
    }

    @ZenMethod
    public static void addSummoningRecipe(String id, IItemStack[] ingredients, String entity, int burnTicks) {
        addSummoningRecipe(id, ingredients, entity, burnTicks, null);
    }

    @ZenMethod
    public static void addSummoningRecipe(String id, IItemStack[] ingredients, String entity, int burnTicks,
        IData data) {
        change("add " + id, rules -> {
            newId(rules, id);
            NBTBase nbt = data == null ? new NBTTagCompound() : NBTConverter.from(data);
            if (!(nbt instanceof NBTTagCompound)) throw new IllegalArgumentException("entity NBT must be a compound");
            rules.add(0, new Recipe(id, items(ingredients), entity, burnTicks, (NBTTagCompound) nbt));
        });
    }

    private static void edit(String id, Consumer<Recipe> operation) {
        change("edit " + id, rules -> {
            int i = find(rules, id);
            Recipe r = rules.get(i)
                .copy();
            operation.accept(r);
            rules.set(i, r);
        });
    }

    @ZenMethod
    public static void setItems(String id, IItemStack[] values) {
        edit(id, r -> r.items = items(values));
    }

    @ZenMethod
    public static void setBurnTicks(String id, int ticks) {
        edit(id, r -> r.burnTicks = ticks);
    }

    @ZenMethod
    public static void setPowerPerTick(String id, float power) {
        edit(id, r -> r.power = power);
    }

    @ZenMethod
    public static void setInBook(String id, boolean visible) {
        edit(id, r -> r.inBook = visible);
    }

    @ZenMethod
    public static void setBookText(String id, String title, String description) {
        edit(id, r -> {
            if (title == null || description == null)
                throw new IllegalArgumentException("book title and description cannot be null");
            r.title = title;
            r.description = description;
        });
    }

    @ZenMethod
    public static void removeRecipe(String id) {
        change("remove " + id, rules -> rules.remove(find(rules, id)));
    }

    private static void change(String description, Consumer<List<Recipe>> operation) {
        MineTweakerAPI.apply(new IUndoableAction() {

            private List<Recipe> before;

            public void apply() {
                before = null;
                try {
                    if (!WitcheryBrazierFeature.isEnabled())
                        throw new IllegalArgumentException("brazier scripting is disabled");
                    List<Recipe> old = BrazierRegistry.recipes(), next = new ArrayList<>(old);
                    operation.accept(next);
                    BrazierRegistry.replace(next);
                    before = old;
                } catch (IllegalArgumentException e) {
                    MineTweakerAPI.logError("Cannot " + description + " for Brazier: " + e.getMessage());
                }
            }

            public boolean canUndo() {
                return before != null;
            }

            public void undo() {
                if (before != null) {
                    BrazierRegistry.replace(before);
                    before = null;
                }
            }

            public String describe() {
                return "Brazier: " + description;
            }

            public String describeUndo() {
                return "Restoring Brazier: " + description;
            }

            public Object getOverrideKey() {
                return null;
            }
        });
    }

    @ZenMethod
    public static void dump() {
        MineTweakerAPI.logInfo("Brazier recipes:");
        for (Recipe r : BrazierRegistry.recipes()) {
            StringBuilder line = new StringBuilder(r.id).append(" items=[");
            for (ItemStack item : r.items) line
                .append(org.fentanylsolutions.thaumicdabblery.feature.witcheryspinningwheel.SpinningRegistry.key(item))
                .append(" ");
            line.append("] ticks=")
                .append(r.burnTicks)
                .append(" powerPerTick=")
                .append(r.power)
                .append(" inBook=")
                .append(r.inBook);
            if (r.entity != null) line.append(" summon=")
                .append(r.entity)
                .append(" NBT=")
                .append(r.nbt);
            else line.append(" effect=")
                .append(r.effect.unlocalizedName);
            MineTweakerAPI.logInfo(line.toString());
        }
    }
}
