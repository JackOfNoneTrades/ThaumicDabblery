package org.fentanylsolutions.thaumicdabblery.feature.witcherybranch;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

public final class VirtualItemUseState {

    private static final Map<EntityPlayer, ItemStack> ACTIVE_BRANCHES = Collections
        .synchronizedMap(new WeakHashMap<>());

    private VirtualItemUseState() {}

    public static boolean isActive(EntityPlayer player) {
        return getVirtualHeldItem(player) != null;
    }

    public static ItemStack getVirtualHeldItem(EntityPlayer player) {
        return player == null ? null : ACTIVE_BRANCHES.get(player);
    }

    static void begin(EntityPlayer player, ItemStack branch) {
        if (player != null && branch != null) {
            ACTIVE_BRANCHES.put(player, branch);
            ThaumicDabblery.debug(
                "[Mystic Branch/virtual item] Installed branch for " + player.getCommandSenderName()
                    + " on "
                    + (player.worldObj.isRemote ? "client" : "server"));
        }
    }

    static void end(EntityPlayer player) {
        if (player != null) {
            boolean removed = ACTIVE_BRANCHES.remove(player) != null;
            ThaumicDabblery.debug(
                "[Mystic Branch/virtual item] Removed branch for " + player.getCommandSenderName()
                    + " on "
                    + (player.worldObj.isRemote ? "client" : "server")
                    + "; wasPresent="
                    + removed);
        }
    }
}
