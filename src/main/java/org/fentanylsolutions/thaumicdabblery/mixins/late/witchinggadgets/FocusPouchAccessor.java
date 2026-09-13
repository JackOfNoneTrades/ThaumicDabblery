package org.fentanylsolutions.thaumicdabblery.mixins.late.witchinggadgets;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import thaumcraft.common.container.ContainerFocusPouch;

@Mixin(value = ContainerFocusPouch.class, remap = false)
public interface FocusPouchAccessor {

    @Accessor("pouch")
    ItemStack td$getPouch();
}
