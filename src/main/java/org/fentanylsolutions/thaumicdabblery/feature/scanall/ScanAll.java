package org.fentanylsolutions.thaumicdabblery.feature.scanall;

import java.util.ArrayList;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import org.fentanylsolutions.thaumicdabblery.feature.customaspects.PrimalDiscovery;
import org.fentanylsolutions.thaumicdabblery.feature.researchscangates.ResearchScanGatesFeature;
import org.fentanylsolutions.thaumicdabblery.feature.researchscangates.ScanGateRegistry;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketSyncAspects;
import thaumcraft.common.lib.network.playerdata.PacketSyncResearch;
import thaumcraft.common.lib.network.playerdata.PacketSyncScannedEntities;
import thaumcraft.common.lib.network.playerdata.PacketSyncScannedItems;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchManager;

public final class ScanAll {

    private ScanAll() {}

    public static void wrap(Map<String, ArrayList<String>> histories, String player) {
        ArrayList<String> history = histories.get(player);
        if (history != null && !(history instanceof AllScanHistory) && history.contains(AllScanHistory.MARKER)) {
            histories.put(player, new AllScanHistory(history));
        }
    }

    public static boolean itemsComplete(String player) {
        return marked(Thaumcraft.proxy.getScannedObjects(), player);
    }

    private static boolean marked(Map<String, ArrayList<String>> histories, String player) {
        ArrayList<String> history = histories.get(player);
        return history != null && history.contains(AllScanHistory.MARKER);
    }

    public static boolean covers(EntityPlayer player, ScanResult scan) {
        if (player == null || scan == null) return false;
        if (scan.type == 1 || PrimalDiscovery.itemScan(scan)) return itemsComplete(player.getCommandSenderName());
        return scan.type == 2 && scan.entity != null
            && marked(Thaumcraft.proxy.getScannedEntities(), player.getCommandSenderName());
    }

    /** Native discovery adds a zero-valued entry and leaves existing amounts intact. */
    public static int complete(EntityPlayerMP player) {
        String name = player.getCommandSenderName();
        PlayerKnowledge knowledge = Thaumcraft.proxy.getPlayerKnowledge();
        // The target is online. Prevent Thaumcraft's offline lazy load from replacing live balances
        // if its research list was cleared independently of its aspect knowledge.
        knowledge.researchCompleted.computeIfAbsent(name, ignored -> new ArrayList<>());
        int discovered = 0;
        for (Aspect aspect : Aspect.aspects.values()) {
            if (PrimalDiscovery.grant(name, aspect, () -> knowledge.addDiscoveredAspect(name, aspect))) discovered++;
        }
        ResearchManager.completeScannedObjectUnsaved(name, AllScanHistory.MARKER);
        ResearchManager.completeScannedEntityUnsaved(name, AllScanHistory.MARKER);
        for (ResearchCategoryList category : ResearchCategories.researchCategories.values()) {
            for (ResearchItem research : category.research.values()) {
                if ((research.isHidden() || research.isLost()) && research.tags != null
                    && research.tags.size() > 0
                    && (nonempty(research.getItemTriggers()) || nonempty(research.getEntityTriggers())
                        || nonempty(research.getAspectTriggers()))
                    && !ResearchManager.isResearchComplete(name, research.key)) {
                    Thaumcraft.proxy.getResearchManager()
                        .completeResearch(player, "@" + research.key);
                }
            }
        }
        if (ResearchScanGatesFeature.isEnabled()) ScanGateRegistry.completeAllScans(player);
        ResearchManager.scheduleSave(player);
        // These sync packets have no per-aspect discoveries, reward sounds or research popups.
        PacketHandler.INSTANCE.sendTo(new PacketSyncAspects(player), player);
        PacketHandler.INSTANCE.sendTo(new PacketSyncResearch(player), player);
        PacketHandler.INSTANCE.sendTo(new PacketSyncScannedItems(player), player);
        PacketHandler.INSTANCE.sendTo(new PacketSyncScannedEntities(player), player);
        return discovered;
    }

    private static boolean nonempty(Object[] values) {
        return values != null && values.length > 0;
    }
}
