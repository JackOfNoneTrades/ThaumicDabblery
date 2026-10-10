package org.fentanylsolutions.thaumicdabblery.feature.construct;

import net.minecraft.item.ItemStack;

import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;

/** A display-only structure, indexed as layers (bottom first), rows, columns. */
public final class ConstructPage extends ResearchPage {

    public final ItemStack[][][] layers;
    public final ItemStack activation;
    public final AspectList cost;
    public final int width, height, depth;

    public ConstructPage(ItemStack[][][] layers, ItemStack activation, AspectList cost) {
        super("");
        height = layers.length;
        depth = layers[0].length;
        width = layers[0][0].length;
        this.layers = new ItemStack[height][depth][width];
        for (int y = 0; y < height; y++) for (int z = 0; z < depth; z++) for (int x = 0; x < width; x++) {
            ItemStack stack = layers[y][z][x];
            this.layers[y][z][x] = stack == null ? null : stack.copy();
        }
        this.activation = activation == null ? null : activation.copy();
        // TC's copy() adds a null/zero entry when the source list is empty.
        this.cost = new AspectList();
        this.cost.aspects.putAll(cost.aspects);
    }
}
