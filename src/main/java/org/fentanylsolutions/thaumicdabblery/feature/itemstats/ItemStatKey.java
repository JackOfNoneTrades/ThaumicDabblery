package org.fentanylsolutions.thaumicdabblery.feature.itemstats;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

final class ItemStatKey {

    private final Item item;
    private final int damage;

    private ItemStatKey(Item item, int damage) {
        this.item = item;
        this.damage = damage;
    }

    static ItemStatKey fromRule(ItemStack stack) {
        return new ItemStatKey(stack.getItem(), getRuleDamage(stack));
    }

    static ItemStatKey exact(ItemStack stack) {
        return new ItemStatKey(stack.getItem(), getRuleDamage(stack));
    }

    static ItemStatKey wildcard(ItemStack stack) {
        return new ItemStatKey(stack.getItem(), OreDictionary.WILDCARD_VALUE);
    }

    static boolean canHaveExactAndWildcard(ItemStack stack) {
        return getRuleDamage(stack) != OreDictionary.WILDCARD_VALUE;
    }

    private static int getRuleDamage(ItemStack stack) {
        return stack.getItem()
            .isDamageable() ? OreDictionary.WILDCARD_VALUE : stack.getItemDamage();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemStatKey)) {
            return false;
        }
        ItemStatKey key = (ItemStatKey) other;
        return item == key.item && damage == key.damage;
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(item) + damage;
    }
}
