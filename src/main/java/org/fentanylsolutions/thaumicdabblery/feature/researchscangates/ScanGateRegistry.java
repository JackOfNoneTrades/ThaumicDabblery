package org.fentanylsolutions.thaumicdabblery.feature.researchscangates;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;
import org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft.ResearchItemAccessor;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketResearchComplete;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.utils.InventoryUtils;

public final class ScanGateRegistry {

    private static final String PROGRESS_MARKER_PREFIX = "@THAUMICDABBLERY.SCAN.";
    private static final Map<String, Gate> GATES = new LinkedHashMap<>();

    private ScanGateRegistry() {}

    public static synchronized Change requireItems(String researchKey, ItemStack[] alternatives) {
        String key = requireResearchKey(researchKey);
        Clause clause = ItemClause.create(key, alternatives);
        return addClause(key, clause);
    }

    public static synchronized Change requireEntities(String researchKey, String[] alternatives) {
        String key = requireResearchKey(researchKey);
        Clause clause = EntityClause.create(key, alternatives);
        return addClause(key, clause);
    }

    public static synchronized Change clear(String researchKey) {
        String key = requireResearchKey(researchKey);
        Gate gate = GATES.remove(key);
        if (gate != null) {
            gate.deactivate();
        }
        return new ClearChange(key, gate);
    }

    public static synchronized boolean hasRevealMarker(String playerName, String researchKey) {
        if (!GATES.containsKey(researchKey)) {
            return true;
        }
        return ResearchManager.isResearchComplete(playerName, researchKey)
            || ResearchManager.isResearchComplete(playerName, revealMarker(researchKey));
    }

    public static synchronized boolean hasIncompleteRequirement(EntityPlayer player, ScanResult scan) {
        ScanTarget target = ScanTarget.from(scan);
        if (player == null || target == null) {
            return false;
        }

        String playerName = player.getCommandSenderName();
        for (Gate gate : GATES.values()) {
            if (hasFinalProgress(playerName, gate.researchKey)) {
                continue;
            }
            for (ClauseEntry entry : gate.clauses.values()) {
                if (entry.clause.matches(target)
                    && !ResearchManager.isResearchComplete(playerName, entry.clause.marker)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static synchronized boolean hasActiveMatch(EntityPlayer player, ScanResult scan) {
        ScanTarget target = ScanTarget.from(scan);
        if (player == null || target == null) {
            return false;
        }

        String playerName = player.getCommandSenderName();
        for (Gate gate : GATES.values()) {
            if (!hasFinalProgress(playerName, gate.researchKey) && gate.matches(target)) {
                return true;
            }
        }
        return false;
    }

    public static synchronized void recordScan(EntityPlayer player, ScanResult scan) {
        ScanTarget target = ScanTarget.from(scan);
        if (player == null || target == null) {
            return;
        }

        String playerName = player.getCommandSenderName();
        boolean matched = false;
        for (Gate gate : GATES.values()) {
            if (hasFinalProgress(playerName, gate.researchKey)) {
                continue;
            }

            boolean matchedGate = false;
            boolean advancedGate = false;
            for (ClauseEntry entry : gate.clauses.values()) {
                if (!entry.clause.matches(target)) {
                    continue;
                }
                matched = true;
                matchedGate = true;
                advancedGate |= completeProgressMarker(player, entry.clause.marker);
            }

            if (advancedGate && !player.worldObj.isRemote) {
                ThaumicDabblery.LOG.info(
                    "Recorded scan-gate progress for {}: {}/{} requirements complete for {}",
                    gate.researchKey,
                    countProgress(playerName, gate),
                    gate.clauses.size(),
                    playerName);
            }

            if (matchedGate && hasAllProgress(playerName, gate)) {
                completeRevealMarker(player, gate.researchKey);
            }
        }

        if (matched && !player.worldObj.isRemote) {
            // Also covers integrated-server recovery scans where the client inserted the
            // shared intermediate marker before the server handled the scan packet.
            ResearchManager.scheduleSave(player);
        }
    }

    public static synchronized void reconcile(EntityPlayerMP player) {
        String playerName = player.getCommandSenderName();
        for (Gate gate : GATES.values()) {
            if (!hasFinalProgress(playerName, gate.researchKey) && hasAllProgress(playerName, gate)) {
                completeRevealMarker(player, gate.researchKey);
            }
        }
    }

    static synchronized int gateCount() {
        return GATES.size();
    }

    static synchronized void synchronizeActivation(boolean enabled) {
        for (Gate gate : GATES.values()) {
            if (enabled) {
                gate.activate();
            } else {
                gate.deactivate();
            }
        }
    }

    private static Change addClause(String researchKey, Clause clause) {
        Gate gate = GATES.get(researchKey);
        if (gate == null) {
            ResearchItem research = requireResearch(researchKey);
            gate = new Gate(research, ResearchState.capture(research));
            if (ResearchScanGatesFeature.isEnabled()) {
                gate.activate();
            }
            GATES.put(researchKey, gate);
        }

        ClauseEntry entry = gate.clauses.get(clause.marker);
        if (entry == null) {
            gate.clauses.put(clause.marker, new ClauseEntry(clause));
        } else {
            entry.references++;
        }
        return new AddChange(researchKey, clause.marker);
    }

    private static String requireResearchKey(String researchKey) {
        if (researchKey == null || researchKey.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Research key cannot be empty");
        }
        String key = researchKey.trim();
        requireResearch(key);
        return key;
    }

    private static ResearchItem requireResearch(String researchKey) {
        ResearchItem research = ResearchCategories.getResearch(researchKey);
        if (research == null) {
            throw new IllegalArgumentException("Unknown Thaumcraft research: " + researchKey);
        }
        return research;
    }

    private static boolean hasFinalProgress(String playerName, String researchKey) {
        return ResearchManager.isResearchComplete(playerName, researchKey)
            || ResearchManager.isResearchComplete(playerName, revealMarker(researchKey));
    }

    private static boolean hasAllProgress(String playerName, Gate gate) {
        if (gate.clauses.isEmpty()) {
            return false;
        }
        for (ClauseEntry entry : gate.clauses.values()) {
            if (!ResearchManager.isResearchComplete(playerName, entry.clause.marker)) {
                return false;
            }
        }
        return true;
    }

    private static int countProgress(String playerName, Gate gate) {
        int completed = 0;
        for (ClauseEntry entry : gate.clauses.values()) {
            if (ResearchManager.isResearchComplete(playerName, entry.clause.marker)) {
                completed++;
            }
        }
        return completed;
    }

    private static boolean completeProgressMarker(EntityPlayer player, String marker) {
        String playerName = player.getCommandSenderName();
        if (ResearchManager.isResearchComplete(playerName, marker)) {
            return false;
        }

        if (player.worldObj.isRemote) {
            ResearchManager.completeResearchUnsaved(playerName, marker);
        } else {
            Thaumcraft.proxy.getResearchManager()
                .completeResearch(player, marker);
        }
        return true;
    }

    private static void completeRevealMarker(EntityPlayer player, String researchKey) {
        String marker = revealMarker(researchKey);
        String playerName = player.getCommandSenderName();
        if (ResearchManager.isResearchComplete(playerName, marker)
            || ResearchManager.isResearchComplete(playerName, researchKey)) {
            return;
        }

        if (player.worldObj.isRemote) {
            return;
        }

        Thaumcraft.proxy.getResearchManager()
            .completeResearch(player, marker);
        PacketHandler.INSTANCE.sendTo(new PacketResearchComplete(marker), (EntityPlayerMP) player);
        ThaumicDabblery.LOG.info("Revealed scan-gated research {} for {}", researchKey, playerName);
    }

    private static String revealMarker(String researchKey) {
        return "@" + researchKey;
    }

    private static String progressMarker(String researchKey, String canonicalClause) {
        return PROGRESS_MARKER_PREFIX + sha256(researchKey + '\0' + canonicalClause);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder(hash.length * 2);
            for (byte part : hash) {
                output.append(Character.forDigit((part >>> 4) & 0xF, 16));
                output.append(Character.forDigit(part & 0xF, 16));
            }
            return output.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String itemIdentity(ItemStack stack) {
        UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(stack.getItem());
        String itemName = identifier == null ? Integer.toString(Item.getIdFromItem(stack.getItem()))
            : identifier.modId + ':' + identifier.name;
        String tag = stack.stackTagCompound == null ? "" : stack.stackTagCompound.toString();
        return itemName + ':' + stack.getItemDamage() + ':' + tag;
    }

    private static ItemStack[] copyItems(ItemStack[] items) {
        if (items == null) {
            return null;
        }
        ItemStack[] result = new ItemStack[items.length];
        for (int index = 0; index < items.length; index++) {
            result[index] = items[index] == null ? null : items[index].copy();
        }
        return result;
    }

    public interface Change {

        void undo();
    }

    private abstract static class Clause {

        private final String marker;

        private Clause(String marker) {
            this.marker = marker;
        }

        abstract boolean matches(ScanTarget target);
    }

    private static final class ItemClause extends Clause {

        private final ItemStack[] alternatives;

        private ItemClause(String marker, ItemStack[] alternatives) {
            super(marker);
            this.alternatives = alternatives;
        }

        private static ItemClause create(String researchKey, ItemStack[] input) {
            if (input == null || input.length == 0) {
                throw new IllegalArgumentException("Item scan requirement cannot be empty");
            }

            Map<String, ItemStack> unique = new LinkedHashMap<>();
            for (ItemStack stack : input) {
                if (stack == null || stack.getItem() == null) {
                    throw new IllegalArgumentException("Item scan requirement cannot contain null");
                }
                ItemStack normalized = stack.copy();
                normalized.stackSize = 1;
                unique.put(itemIdentity(normalized), normalized);
            }

            List<String> identities = new ArrayList<>(unique.keySet());
            Collections.sort(identities);
            ItemStack[] alternatives = new ItemStack[identities.size()];
            for (int index = 0; index < identities.size(); index++) {
                alternatives[index] = unique.get(identities.get(index));
            }
            String canonical = "ITEM|" + String.join("|", identities);
            return new ItemClause(progressMarker(researchKey, canonical), alternatives);
        }

        @Override
        boolean matches(ScanTarget target) {
            if (target.item == null) {
                return false;
            }
            for (ItemStack alternative : alternatives) {
                if (InventoryUtils.areItemStacksEqual(alternative, target.item, true, true, false)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class EntityClause extends Clause {

        private final Set<String> alternatives;

        private EntityClause(String marker, Set<String> alternatives) {
            super(marker);
            this.alternatives = alternatives;
        }

        private static EntityClause create(String researchKey, String[] input) {
            if (input == null || input.length == 0) {
                throw new IllegalArgumentException("Entity scan requirement cannot be empty");
            }

            Set<String> unique = new LinkedHashSet<>();
            for (String entity : input) {
                if (entity == null || entity.trim()
                    .isEmpty()) {
                    throw new IllegalArgumentException("Entity scan requirement cannot contain an empty id");
                }
                unique.add(entity.trim());
            }

            List<String> identities = new ArrayList<>(unique);
            Collections.sort(identities);
            String canonical = "ENTITY|" + String.join("|", identities);
            return new EntityClause(progressMarker(researchKey, canonical), new LinkedHashSet<>(identities));
        }

        @Override
        boolean matches(ScanTarget target) {
            return target.entityId != null && alternatives.contains(target.entityId);
        }
    }

    private static final class ClauseEntry {

        private final Clause clause;
        private int references = 1;

        private ClauseEntry(Clause clause) {
            this.clause = clause;
        }
    }

    private static final class ScanTarget {

        private final ItemStack item;
        private final String entityId;

        private ScanTarget(ItemStack item, String entityId) {
            this.item = item;
            this.entityId = entityId;
        }

        private static ScanTarget from(ScanResult scan) {
            if (scan == null) {
                return null;
            }
            if (scan.type == 1) {
                Item item = Item.getItemById(scan.id);
                return item == null ? null : new ScanTarget(new ItemStack(item, 1, scan.meta), null);
            }
            if (scan.type != 2 || scan.entity == null) {
                return null;
            }

            Entity entity = scan.entity;
            if (entity instanceof EntityItem) {
                ItemStack stack = ((EntityItem) entity).getEntityItem();
                if (stack == null || stack.getItem() == null) {
                    return null;
                }
                ItemStack normalized = stack.copy();
                normalized.stackSize = 1;
                return new ScanTarget(normalized, null);
            }
            return new ScanTarget(null, EntityList.getEntityString(entity));
        }
    }

    private static final class Gate {

        private final String researchKey;
        private final ResearchItem research;
        private final ResearchState originalState;
        private final Map<String, ClauseEntry> clauses = new LinkedHashMap<>();
        private boolean active;

        private Gate(ResearchItem research, ResearchState originalState) {
            this.researchKey = research.key;
            this.research = research;
            this.originalState = originalState;
        }

        private void activate() {
            if (active) {
                return;
            }
            research.setItemTriggers((ItemStack[]) null);
            research.setEntityTriggers((String[]) null);
            research.setAspectTriggers((Aspect[]) null);
            ResearchItemAccessor accessor = (ResearchItemAccessor) (Object) research;
            accessor.thaumicdabblery$setHidden(false);
            accessor.thaumicdabblery$setLost(true);
            active = true;
        }

        private void deactivate() {
            if (active) {
                originalState.restore(research);
                active = false;
            }
        }

        private boolean matches(ScanTarget target) {
            for (ClauseEntry entry : clauses.values()) {
                if (entry.clause.matches(target)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class ResearchState {

        private final boolean hidden;
        private final boolean lost;
        private final ItemStack[] itemTriggers;
        private final String[] entityTriggers;
        private final Aspect[] aspectTriggers;

        private ResearchState(boolean hidden, boolean lost, ItemStack[] itemTriggers, String[] entityTriggers,
            Aspect[] aspectTriggers) {
            this.hidden = hidden;
            this.lost = lost;
            this.itemTriggers = itemTriggers;
            this.entityTriggers = entityTriggers;
            this.aspectTriggers = aspectTriggers;
        }

        private static ResearchState capture(ResearchItem research) {
            ResearchItemAccessor accessor = (ResearchItemAccessor) (Object) research;
            return new ResearchState(
                accessor.thaumicdabblery$isHidden(),
                accessor.thaumicdabblery$isLost(),
                copyItems(research.getItemTriggers()),
                research.getEntityTriggers() == null ? null
                    : research.getEntityTriggers()
                        .clone(),
                research.getAspectTriggers() == null ? null
                    : research.getAspectTriggers()
                        .clone());
        }

        private void restore(ResearchItem research) {
            research.setItemTriggers(copyItems(itemTriggers));
            research.setEntityTriggers(entityTriggers == null ? null : entityTriggers.clone());
            research.setAspectTriggers(aspectTriggers == null ? null : aspectTriggers.clone());
            ResearchItemAccessor accessor = (ResearchItemAccessor) (Object) research;
            accessor.thaumicdabblery$setHidden(hidden);
            accessor.thaumicdabblery$setLost(lost);
        }
    }

    private static final class AddChange implements Change {

        private final String researchKey;
        private final String marker;
        private boolean undone;

        private AddChange(String researchKey, String marker) {
            this.researchKey = researchKey;
            this.marker = marker;
        }

        @Override
        public void undo() {
            synchronized (ScanGateRegistry.class) {
                if (undone) {
                    return;
                }
                Gate gate = GATES.get(researchKey);
                if (gate != null) {
                    ClauseEntry entry = gate.clauses.get(marker);
                    if (entry != null && --entry.references == 0) {
                        gate.clauses.remove(marker);
                    }
                    if (gate.clauses.isEmpty()) {
                        GATES.remove(researchKey);
                        gate.deactivate();
                    }
                }
                undone = true;
            }
        }
    }

    private static final class ClearChange implements Change {

        private final String researchKey;
        private final Gate removed;
        private boolean undone;

        private ClearChange(String researchKey, Gate removed) {
            this.researchKey = researchKey;
            this.removed = removed;
        }

        @Override
        public void undo() {
            synchronized (ScanGateRegistry.class) {
                if (undone || removed == null) {
                    undone = true;
                    return;
                }
                if (GATES.containsKey(researchKey)) {
                    throw new IllegalStateException("Cannot restore cleared scan gate for " + researchKey);
                }
                if (ResearchScanGatesFeature.isEnabled()) {
                    removed.activate();
                }
                GATES.put(researchKey, removed);
                undone = true;
            }
        }
    }
}
