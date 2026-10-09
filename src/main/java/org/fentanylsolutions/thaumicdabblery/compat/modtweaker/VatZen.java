package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.Locale;
import java.util.Map;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.vatfacing.VatAppearance;
import org.fentanylsolutions.thaumicdabblery.feature.vatfacing.VatFacing;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IItemStack;
import modtweaker2.helpers.InputHelper;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.thaumichorizons.Vat")
public final class VatZen {

    public static void register() {
        MineTweakerAPI.registerClass(VatZen.class);
    }

    @ZenMethod
    public static void setTracking(String entity, String mode) {
        setTracking(entity, mode, 8, 60, 30, 6);
    }

    @ZenMethod
    public static void setTracking(String entity, String mode, double range, double maxYaw, double maxPitch,
        double speed) {
        requireEntity(entity);
        String normalized = mode == null ? "" : mode.toLowerCase(Locale.ROOT);
        if (!normalized.equals("none") && !normalized.equals("head") && !normalized.equals("body"))
            throw new IllegalArgumentException("Vat tracking mode must be head, body or none");
        if (!valid(range, 0.1, 64) || !valid(maxYaw, 0, 85) || !valid(maxPitch, 0, 85) || !valid(speed, 0.1, 180))
            throw new IllegalArgumentException(
                "Vat tracking requires range 0.1..64, head limits 0..85 degrees and speed 0.1..180 degrees/tick");
        VatFacing.Rule rule = new VatFacing.Rule(normalized, range, (float) maxYaw, (float) maxPitch, (float) speed);
        MineTweakerAPI.apply(new Change("vat tracking for " + entity) {

            VatFacing.Rule previous;

            public void apply() {
                previous = VatFacing.RULES.put(entity, rule);
            }

            public void undo() {
                if (previous == null) VatFacing.RULES.remove(entity);
                else VatFacing.RULES.put(entity, previous);
            }
        });
    }

    private static void requireEntity(String entity) {
        Class<?> type = (Class<?>) EntityList.stringToClassMapping.get(entity);
        if (!"effigy".equals(entity) && (type == null || !EntityLivingBase.class.isAssignableFrom(type)
            || EntityPlayer.class.isAssignableFrom(type)))
            throw new IllegalArgumentException("Unknown vat creature: " + entity);
    }

    /** Global default; entity-specific overrides win regardless of definition order. */
    @ZenMethod
    public static void setBobbing(double amplitude, int periodTicks) {
        VatAppearance.Bobbing setting = bobbing(amplitude, periodTicks);
        MineTweakerAPI.apply(new Change("global vat bobbing") {

            VatAppearance.Bobbing previous;

            public void apply() {
                previous = VatAppearance.globalBobbing;
                VatAppearance.globalBobbing = setting;
            }

            public void undo() {
                VatAppearance.globalBobbing = previous;
            }
        });
    }

    @ZenMethod
    public static void setBobbing(String entity, double amplitude, int periodTicks) {
        requireEntity(entity);
        setAppearance(VatAppearance.BOBBING, entity, bobbing(amplitude, periodTicks), "bobbing");
    }

    @ZenMethod
    public static void setYOffset(String entity, double offset) {
        requireEntity(entity);
        if (!valid(offset, -4, 4)) throw new IllegalArgumentException("Vat Y offset must be -4..4 blocks");
        setAppearance(VatAppearance.Y_OFFSETS, entity, (float) offset, "Y offset");
    }

    @ZenMethod
    public static void setScale(String entity, double scale) {
        requireEntity(entity);
        if (!valid(scale, 0.05, 8)) throw new IllegalArgumentException("Vat scale must be 0.05..8");
        setAppearance(VatAppearance.SCALES, entity, (float) scale, "scale");
    }

    private static VatAppearance.Bobbing bobbing(double amplitude, int periodTicks) {
        if (!valid(amplitude, 0, 2) || periodTicks < 2 || periodTicks > 72000)
            throw new IllegalArgumentException("Vat bobbing requires amplitude 0..2 blocks and period 2..72000 ticks");
        return new VatAppearance.Bobbing((float) amplitude, periodTicks);
    }

    private static <T> void setAppearance(Map<String, T> settings, String entity, T value, String description) {
        MineTweakerAPI.apply(new Change("vat " + description + " for " + entity) {

            T previous;

            public void apply() {
                previous = settings.put(entity, value);
            }

            public void undo() {
                if (previous == null) settings.remove(entity);
                else settings.put(entity, previous);
            }
        });
    }

    private static boolean valid(double value, double min, double max) {
        return !Double.isNaN(value) && value >= min && value <= max;
    }

    @ZenMethod
    public static void setRotationItem(IItemStack item) {
        ItemStack stack = item == null ? null : InputHelper.toStack(item);
        if (item != null && (stack == null || stack.getItem() == null))
            throw new IllegalArgumentException("Invalid vat rotation item");
        MineTweakerAPI.apply(new Change("vat rotation item") {

            ItemStack previous;

            public void apply() {
                previous = VatFacing.rotationItem;
                VatFacing.rotationItem = stack == null ? null : stack.copy();
            }

            public void undo() {
                VatFacing.rotationItem = previous;
            }
        });
    }

    private abstract static class Change implements IUndoableAction {

        private final String description;

        Change(String description) {
            this.description = description;
        }

        public boolean canUndo() {
            return true;
        }

        public String describe() {
            return "Apply " + description;
        }

        public String describeUndo() {
            return "Restore " + description;
        }

        public Object getOverrideKey() {
            return null;
        }
    }
}
