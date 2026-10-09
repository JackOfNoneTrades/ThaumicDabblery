package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.Locale;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

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
        Class<?> type = (Class<?>) EntityList.stringToClassMapping.get(entity);
        if (!"effigy".equals(entity) && (type == null || !EntityLivingBase.class.isAssignableFrom(type)
            || EntityPlayer.class.isAssignableFrom(type)))
            throw new IllegalArgumentException("Unknown vat creature: " + entity);
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
