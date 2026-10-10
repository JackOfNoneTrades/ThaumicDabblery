package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.witcheryrites.WitcheryRitesFeature;
import org.fentanylsolutions.thaumicdabblery.mixins.late.witchery.RitualAccessor;
import org.fentanylsolutions.thaumicdabblery.mixins.late.witchery.SacrificeItemAccessor;
import org.fentanylsolutions.thaumicdabblery.mixins.late.witchery.SacrificeLivingAccessor;
import org.fentanylsolutions.thaumicdabblery.mixins.late.witchery.SacrificeMultipleAccessor;

import com.emoniph.witchery.ritual.Circle;
import com.emoniph.witchery.ritual.RiteRegistry;
import com.emoniph.witchery.ritual.RitualTraits;
import com.emoniph.witchery.ritual.Sacrifice;
import com.emoniph.witchery.ritual.SacrificeItem;
import com.emoniph.witchery.ritual.SacrificeLiving;
import com.emoniph.witchery.ritual.SacrificeMultiple;
import com.emoniph.witchery.ritual.SacrificeOptionalItem;
import com.emoniph.witchery.ritual.SacrificePower;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/** Requirement edits deliberately leave registry slots and native effect objects untouched. */
@ZenClass("mods.witchery.Rites")
public final class WitcheryRitesZen {

    private WitcheryRitesZen() {}

    public static void init() {
        MineTweakerAPI.registerClass(WitcheryRitesZen.class);
    }

    public static void registerCommand() {
        if (MineTweakerAPI.server != null) MineTweakerAPI.server.addMineTweakerCommand(
            "witcheryRites",
            new String[] { "/mt witcheryRites", "    Lists Witchery rite IDs and requirements in minetweaker.log" },
            (arguments, player) -> {
                dump();
                if (player != null) player.sendChat(
                    MineTweakerImplementationAPI.platform.getMessage("Witchery rites written to minetweaker.log"));
            });
    }

    @ZenMethod
    public static void setItems(int id, IItemStack[] items) {
        sacrifice(id, "items", SacrificeItem.class, () -> new SacrificeItem(stacks(items)));
    }

    @ZenMethod
    public static void setOptionalItems(int id, IItemStack[] items) {
        sacrifice(id, "optional items", SacrificeOptionalItem.class, () -> {
            List<Sacrifice> result = new ArrayList<>();
            for (ItemStack item : stacks(items)) result.add(new SacrificeOptionalItem(item));
            return new SacrificeMultiple(result.toArray(new Sacrifice[0]));
        });
    }

    private static ItemStack[] stacks(IItemStack[] values) {
        if (values == null) throw new IllegalArgumentException("items must be an array; use [] to remove them");
        ItemStack[] result = new ItemStack[values.length];
        for (int i = 0; i < values.length; i++) {
            ItemStack stack = values[i] == null ? null : MineTweakerMC.getItemStack(values[i]);
            if (stack == null || stack.getItem() == null || stack.stackSize != 1 || stack.getItemDamage() == 32767)
                throw new IllegalArgumentException(
                    "offerings must be single items with exact metadata; repeat entries for multiple sacrifices");
            if (stack.hasTagCompound())
                throw new IllegalArgumentException("Witchery offerings do not match custom NBT; use an untagged item");
            result[i] = stack.copy();
        }
        return result;
    }

    @ZenMethod
    public static void setLivingSacrifices(int id, String[] entities) {
        sacrifice(id, "living sacrifices", SacrificeLiving.class, () -> {
            if (entities == null)
                throw new IllegalArgumentException("entities must be an array; use [] to remove them");
            List<Sacrifice> result = new ArrayList<>();
            for (String name : entities) {
                Class<?> type = (Class<?>) EntityList.stringToClassMapping.get(name);
                if (type == null || !EntityLiving.class.isAssignableFrom(type))
                    throw new IllegalArgumentException("unknown living mob: " + name + "; players are not supported");
                result.add(new SacrificeLiving(type.asSubclass(EntityLiving.class)));
            }
            return new SacrificeMultiple(result.toArray(new Sacrifice[0]));
        });
    }

    @ZenMethod
    public static void setPower(int id, float amount) {
        change(id, "initial altar power", ritual -> {
            if (!Float.isFinite(amount) || amount < 0)
                throw new IllegalArgumentException("power must be finite and nonnegative");
            Sacrifice before = ritual.td$getSacrifice();
            List<Sacrifice> parts = new ArrayList<>();
            flatten(before, parts);
            int interval = 20;
            for (Sacrifice part : parts) if (part.getClass() == SacrificePower.class) {
                interval = ((SacrificePower) part).powerFrequencyInTicks;
                break;
            }
            Sacrifice replacement = amount == 0 ? new SacrificeMultiple() : new SacrificePower(amount, interval);
            Sacrifice after = replace(before, SacrificePower.class, replacement);
            return edit(() -> ritual.td$setSacrifice(after), () -> ritual.td$setSacrifice(before));
        });
    }

    /** Native homogeneous circles only: small/medium/large followed by white/otherwhere/infernal. */
    @ZenMethod
    public static void setCircles(int id, String[] circles) {
        change(id, "circles", ritual -> {
            if (circles == null) throw new IllegalArgumentException("circles must be an array; use [] for none");
            Circle[] after = new Circle[circles.length];
            boolean[] seen = new boolean[3];
            for (int i = 0; i < circles.length; i++) {
                String[] words = normalized(circles[i]).split("\\s+");
                if (words.length != 2)
                    throw new IllegalArgumentException("expected circles such as 'small white' or 'large infernal'");
                int size = Arrays.asList("small", "medium", "large")
                    .indexOf(words[0]);
                int chalk = Arrays.asList("white", "otherwhere", "infernal")
                    .indexOf(words[1]);
                if (size < 0 || chalk < 0) throw new IllegalArgumentException("unknown circle: " + circles[i]);
                if (seen[size]) throw new IllegalArgumentException("only one circle of each size is possible");
                seen[size] = true;
                int glyphs = 16 + size * 12;
                after[i] = new Circle(chalk == 0 ? glyphs : 0, chalk == 1 ? glyphs : 0, chalk == 2 ? glyphs : 0);
            }
            Circle[] before = ritual.td$getCircles();
            return edit(() -> ritual.td$setCircles(after), () -> ritual.td$setCircles(before));
        });
    }

    @ZenMethod
    public static void setTime(int id, String time) {
        traits(id, "time", values -> {
            String value = normalized(time);
            if (!Arrays.asList("any", "day", "night")
                .contains(value)) throw new IllegalArgumentException("time must be any, day or night");
            values.remove(RitualTraits.ONLY_AT_DAY);
            values.remove(RitualTraits.ONLY_AT_NIGHT);
            if (value.equals("day")) values.add(RitualTraits.ONLY_AT_DAY);
            if (value.equals("night")) values.add(RitualTraits.ONLY_AT_NIGHT);
        });
    }

    @ZenMethod
    public static void setWeather(int id, String weather) {
        traits(id, "weather", values -> {
            String value = normalized(weather);
            if (!Arrays.asList("any", "rain", "thunderstorm")
                .contains(value)) throw new IllegalArgumentException("weather must be any, rain or thunderstorm");
            values.remove(RitualTraits.ONLY_IN_RAIN);
            values.remove(RitualTraits.ONLY_IN_STROM);
            if (value.equals("rain")) values.add(RitualTraits.ONLY_IN_RAIN);
            if (value.equals("thunderstorm")) values.add(RitualTraits.ONLY_IN_STROM);
        });
    }

    @ZenMethod
    public static void setOverworldOnly(int id, boolean enabled) {
        toggle(id, RitualTraits.ONLY_OVERWORLD, enabled);
    }

    private static void toggle(int id, RitualTraits trait, boolean enabled) {
        traits(id, trait.name(), values -> {
            if (enabled) values.add(trait);
            else values.remove(trait);
        });
    }

    private static String normalized(String value) {
        if (value == null) throw new IllegalArgumentException("value must not be null");
        return value.trim()
            .toLowerCase(Locale.ROOT);
    }

    private static void traits(int id, String description, Consumer<EnumSet<RitualTraits>> operation) {
        change(id, description, ritual -> {
            EnumSet<RitualTraits> before = ritual.td$getTraits();
            EnumSet<RitualTraits> after = before.clone();
            operation.accept(after);
            return edit(() -> ritual.td$setTraits(after), () -> ritual.td$setTraits(before));
        });
    }

    private static void sacrifice(int id, String description, Class<? extends Sacrifice> kind,
        Supplier<Sacrifice> replacement) {
        change(id, description, ritual -> {
            Sacrifice before = ritual.td$getSacrifice();
            Sacrifice after = replace(before, kind, replacement.get());
            return edit(() -> ritual.td$setSacrifice(after), () -> ritual.td$setSacrifice(before));
        });
    }

    // Only unwrap the native composite. Unknown subclasses may have additional behavior.
    private static void flatten(Sacrifice node, List<Sacrifice> result) {
        if (node != null && node.getClass() == SacrificeMultiple.class) {
            for (Sacrifice child : ((SacrificeMultipleAccessor) node).td$getSacrifices()) flatten(child, result);
        } else if (node != null) result.add(node);
    }

    private static Sacrifice replace(Sacrifice root, Class<? extends Sacrifice> kind, Sacrifice replacement) {
        List<Sacrifice> parts = new ArrayList<>();
        flatten(root, parts);
        List<Sacrifice> result = new ArrayList<>();
        boolean inserted = false;
        for (Sacrifice part : parts) {
            if (part.getClass() == kind) {
                if (!inserted) flatten(replacement, result);
                inserted = true;
            } else result.add(part);
        }
        if (!inserted) flatten(replacement, result);
        return new SacrificeMultiple(result.toArray(new Sacrifice[0]));
    }

    private interface Operation {

        Runnable[] prepare(RitualAccessor ritual);
    }

    private static Runnable[] edit(Runnable apply, Runnable undo) {
        return new Runnable[] { apply, undo };
    }

    private static void change(int id, String description, Operation operation) {
        MineTweakerAPI.apply(new IUndoableAction() {

            private Runnable undo;

            public void apply() {
                undo = null;
                try {
                    if (!WitcheryRitesFeature.isEnabled())
                        throw new IllegalArgumentException("Witchery rite editing is disabled in the config");
                    RiteRegistry.Ritual target = null;
                    for (RiteRegistry.Ritual ritual : RiteRegistry.instance()
                        .getRituals()) if (ritual.getRitualID() == id) {
                            if (target != null) throw new IllegalArgumentException("duplicate rite ID " + id);
                            target = ritual;
                        }
                    if (target == null)
                        throw new IllegalArgumentException("unknown rite ID " + id + "; use /mt witcheryRites");
                    Runnable[] edit = operation.prepare((RitualAccessor) target);
                    edit[0].run();
                    undo = edit[1];
                } catch (IllegalArgumentException error) {
                    MineTweakerAPI
                        .logError("Cannot edit Witchery rite " + id + " (" + description + "): " + error.getMessage());
                }
            }

            public boolean canUndo() {
                return undo != null;
            }

            public void undo() {
                if (undo != null) {
                    undo.run();
                    undo = null;
                }
            }

            public String describe() {
                return "Editing Witchery rite " + id + ": " + description;
            }

            public String describeUndo() {
                return "Restoring Witchery rite " + id + ": " + description;
            }

            public Object getOverrideKey() {
                return null;
            }
        });
    }

    @ZenMethod
    public static void dump() {
        for (RiteRegistry.Ritual ritual : RiteRegistry.instance()
            .getRituals()) {
            RitualAccessor data = (RitualAccessor) ritual;
            MineTweakerAPI.logCommand(
                "Rite " + ritual
                    .getRitualID() + ": " + ritual.getLocalizedName() + " [" + ritual.getUnlocalizedName() + "]");
            List<String> circles = new ArrayList<>();
            for (Circle circle : data.td$getCircles()) {
                int texture = circle.getTextureIndex();
                circles.add(
                    circle.getExclusiveMetadataValue() != 0 && texture >= 0 && texture <= 8
                        ? new String[] { "large", "medium", "small" }[texture / 3] + " "
                            + new String[] { "white", "otherwhere", "infernal" }[texture % 3]
                        : "custom circle");
            }
            MineTweakerAPI.logCommand("  Circles: " + circles + "; conditions: " + data.td$getTraits());
            List<Sacrifice> parts = new ArrayList<>();
            flatten(data.td$getSacrifice(), parts);
            for (Sacrifice part : parts) {
                String detail = part.getClass()
                    .getSimpleName();
                if (part instanceof SacrificeItem) for (ItemStack item : ((SacrificeItemAccessor) part).td$getItems())
                    detail += " <" + ItemStackName.name(item) + ">";
                if (part instanceof SacrificeLiving) detail += " "
                    + EntityList.classToStringMapping.get(((SacrificeLivingAccessor) part).td$getEntityClass());
                if (part instanceof SacrificePower)
                    detail += " " + ((SacrificePower) part).powerRequired + " (initial cost)";
                MineTweakerAPI.logCommand("  " + detail);
            }
        }
    }

    private static final class ItemStackName {

        private static String name(ItemStack stack) {
            return net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem()) + ":" + stack.getItemDamage();
        }
    }
}
