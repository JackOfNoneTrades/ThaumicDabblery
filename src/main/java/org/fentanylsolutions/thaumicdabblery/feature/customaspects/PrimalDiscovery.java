package org.fentanylsolutions.thaumicdabblery.feature.customaspects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ScanManager;

/**
 * Discovery is per player. Scoped grants are used only for successful item scans and authoritative saved/network data.
 */
public final class PrimalDiscovery {

    private static final ThreadLocal<String> GRANT = new ThreadLocal<>();

    private PrimalDiscovery() {}

    public static boolean allowed(PlayerKnowledge knowledge, String player, Aspect aspect) {
        if (!CustomAspectRegistry.isHidden(aspect)) return true;
        AspectList known = knowledge.aspectsDiscovered.get(player);
        return known != null && known.aspects.containsKey(aspect)
            || (player + ":" + aspect.getTag()).equals(GRANT.get());
    }

    public static <T> T grant(String player, Aspect aspect, Supplier<T> operation) {
        String previous = GRANT.get();
        GRANT.set(player + ":" + (aspect == null ? "" : aspect.getTag()));
        try {
            return operation.get();
        } finally {
            if (previous == null) GRANT.remove();
            else GRANT.set(previous);
        }
    }

    public static boolean itemScan(ScanResult scan) {
        return scan.type == 1 || scan.type == 2 && scan.entity instanceof EntityItem;
    }

    public static boolean hasUnknownPrimal(EntityPlayer player, ScanResult scan) {
        if (!itemScan(scan)) return false;
        AspectList aspects = ScanManager.getScanAspects(scan, player.worldObj);
        if (aspects == null) return false;
        for (Aspect aspect : aspects.getAspects())
            if (!allowed(Thaumcraft.proxy.getPlayerKnowledge(), player.getCommandSenderName(), aspect)) return true;
        return false;
    }

    public static void revealScannedPrimals(EntityPlayer player, ScanResult scan, String prefix) {
        if (player.worldObj.isRemote || !itemScan(scan)) return;
        AspectList aspects = ScanManager.getScanAspects(scan, player.worldObj);
        if (!ScanManager.validScan(aspects, player)) return;
        for (Aspect aspect : aspects.getAspects()) {
            if (!allowed(Thaumcraft.proxy.getPlayerKnowledge(), player.getCommandSenderName(), aspect)) {
                int amount = aspects.getAmount(aspect) + ("#".equals(prefix) ? 1 : 0);
                grant(
                    player.getCommandSenderName(),
                    aspect,
                    () -> ScanManager.checkAndSyncAspectKnowledge(player, aspect, amount));
            }
        }
    }

    public static ArrayList<Aspect> nativePrimals() {
        return new ArrayList<>(
            Arrays.asList(Aspect.AIR, Aspect.EARTH, Aspect.FIRE, Aspect.WATER, Aspect.ORDER, Aspect.ENTROPY));
    }

    public static ArrayList<Aspect> visiblePrimals(EntityPlayer player) {
        ArrayList<Aspect> result = Aspect.getPrimalAspects();
        result
            .removeIf(aspect -> !allowed(Thaumcraft.proxy.getPlayerKnowledge(), player.getCommandSenderName(), aspect));
        return result;
    }

    public static AspectList visibleVis(AspectList vis, EntityPlayer player) {
        AspectList result = vis.copy();
        for (Aspect aspect : vis.getAspects())
            if (!allowed(Thaumcraft.proxy.getPlayerKnowledge(), player.getCommandSenderName(), aspect))
                result.remove(aspect);
        return result;
    }
}
