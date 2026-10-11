package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.witcherykettle.KettleRegistry;
import org.fentanylsolutions.thaumicdabblery.feature.witcherykettle.KettleRegistry.Recipe;
import org.fentanylsolutions.thaumicdabblery.feature.witcherykettle.WitcheryKettleFeature;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.witchery.Kettle")
public final class WitcheryKettleZen {

    private WitcheryKettleZen() {}

    public static void init() {
        MineTweakerAPI.registerClass(WitcheryKettleZen.class);
    }

    public static void registerCommand() {
        if (MineTweakerAPI.server != null) MineTweakerAPI.server.addMineTweakerCommand(
            "witcheryKettle",
            new String[] { "/mt witcheryKettle",
                "    Lists kettle recipe IDs, ingredients, outputs and requirements in minetweaker.log" },
            (args, player) -> {
                dump();
                if (player != null) player.sendChat(
                    MineTweakerImplementationAPI.platform
                        .getMessage("Witches' Kettle recipes written to minetweaker.log"));
            });
    }

    private static ItemStack input(IItemStack value) {
        if (value == null || value.hasTransformers())
            throw new IllegalArgumentException("expected a single exact item without transformations");
        return KettleRegistry.input(MineTweakerMC.getItemStack(value));
    }

    private static ItemStack[] inputs(IItemStack[] values) {
        if (values == null || values.length != 6)
            throw new IllegalArgumentException("expected exactly six ingredients");
        ItemStack[] result = new ItemStack[6];
        for (int i = 0; i < 6; i++) result[i] = input(values[i]);
        return result;
    }

    private static ItemStack output(IItemStack value) {
        ItemStack s = value == null ? null : MineTweakerMC.getItemStack(value);
        return s == null ? null : s.copy();
    }

    private static int find(List<Recipe> rules, String id) {
        for (int i = 0; i < rules.size(); i++) if (id != null && id.equals(rules.get(i).id)) return i;
        throw new IllegalArgumentException("unknown kettle recipe: " + id);
    }

    @ZenMethod
    public static void addRecipe(String id, IItemStack[] ingredients, IItemStack output,
        @Optional(valueDouble = 0) float power) {
        change("add " + id, rules -> {
            if (id == null || !id.matches("custom:[A-Za-z0-9_./-]+"))
                throw new IllegalArgumentException("recipe ID must start with custom:");
            for (Recipe r : rules)
                if (id.equals(r.id)) throw new IllegalArgumentException("duplicate recipe ID: " + id);
            rules.add(0, new Recipe(id, inputs(ingredients), output(output), power));
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
    public static void setIngredients(String id, IItemStack[] values) {
        edit(id, r -> r.inputs = inputs(values));
    }

    @ZenMethod
    public static void setOutput(String id, IItemStack value) {
        edit(id, r -> r.output = output(value));
    }

    @ZenMethod
    public static void setPower(String id, float value) {
        edit(id, r -> r.power = value);
    }

    @ZenMethod
    public static void setColor(String id, int rgb) {
        edit(id, r -> {
            if (rgb < 0 || rgb > 0xFFFFFF)
                throw new IllegalArgumentException("color must be RGB from 0x000000 to 0xFFFFFF");
            r.color = 0xFF000000 | rgb;
        });
    }

    @ZenMethod
    public static void setDimension(String id, int value) {
        edit(id, r -> r.dimension = value);
    }

    @ZenMethod
    public static void clearDimension(String id) {
        edit(id, r -> r.dimension = null);
    }

    @ZenMethod
    public static void setFamiliar(String id, String value) {
        edit(id, r -> r.familiar = KettleRegistry.familiar(value));
    }

    @ZenMethod
    public static void setBonus(String id, int value) {
        edit(id, r -> r.bonus = value);
    }

    @ZenMethod
    public static void setInBook(String id, boolean value) {
        edit(id, r -> r.inBook = value);
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
                    if (!WitcheryKettleFeature.isEnabled())
                        throw new IllegalArgumentException("kettle scripting is disabled");
                    List<Recipe> old = KettleRegistry.recipes(), next = new ArrayList<>(old);
                    operation.accept(next);
                    KettleRegistry.replace(next);
                    before = old;
                } catch (IllegalArgumentException e) {
                    MineTweakerAPI.logError("Cannot " + description + " for Witches' Kettle: " + e.getMessage());
                }
            }

            public boolean canUndo() {
                return before != null;
            }

            public void undo() {
                if (before != null) {
                    KettleRegistry.replace(before);
                    before = null;
                }
            }

            public String describe() {
                return "Witches' Kettle: " + description;
            }

            public String describeUndo() {
                return "Restoring Witches' Kettle: " + description;
            }

            public Object getOverrideKey() {
                return null;
            }
        });
    }

    @ZenMethod
    public static void dump() {
        MineTweakerAPI.logInfo("Witches' Kettle recipes:");
        for (Recipe r : KettleRegistry.recipes()) {
            StringBuilder line = new StringBuilder(r.id).append(" ingredients=[");
            for (ItemStack input : r.inputs) line.append(KettleRegistry.key(input))
                .append(" ");
            line.append("] -> ")
                .append(KettleRegistry.key(r.output))
                .append(" * ")
                .append(r.output.stackSize)
                .append("; power=")
                .append(r.power)
                .append("; RGB=")
                .append(String.format("%06X", r.color & 0xFFFFFF))
                .append("; dimension=")
                .append(r.dimension == null ? "any" : r.dimension)
                .append("; familiar=")
                .append(KettleRegistry.familiarName(r.familiar))
                .append("; bonus=")
                .append(r.bonus)
                .append("; inBook=")
                .append(r.inBook);
            if (r.output.hasTagCompound()) line.append("; output NBT=")
                .append(r.output.getTagCompound());
            MineTweakerAPI.logInfo(line.toString());
        }
    }
}
