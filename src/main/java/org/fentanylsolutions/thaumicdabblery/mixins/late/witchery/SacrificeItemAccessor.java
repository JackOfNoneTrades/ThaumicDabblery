package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.emoniph.witchery.ritual.SacrificeItem;

@Mixin(value = SacrificeItem.class, remap = false)
public interface SacrificeItemAccessor {

    @Accessor("itemstacks")
    ItemStack[] td$getItems();
}
