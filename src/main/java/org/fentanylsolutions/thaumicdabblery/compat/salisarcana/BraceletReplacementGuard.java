package org.fentanylsolutions.thaumicdabblery.compat.salisarcana;

import net.minecraft.item.ItemStack;

/** Bracelets use metadata for their variant, not the assembly cost written by Salis's replacement recipes. */
public final class BraceletReplacementGuard {

    private BraceletReplacementGuard() {}

    public static boolean isBracelet(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        // Walk the hierarchy to include addon subclasses without linking either optional bracelet mod.
        for (Class<?> type = stack.getItem()
            .getClass(); type != null; type = type.getSuperclass()) {
            String name = type.getName();
            if (name.equals("tb.common.item.ItemCastingBracelet")
                || name.equals("com.ilya3point999k.thaumicconcilium.common.items.wands.Bracelets")) return true;
        }
        return false;
    }
}
