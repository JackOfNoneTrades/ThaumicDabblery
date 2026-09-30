package org.fentanylsolutions.thaumicdabblery.feature.scanall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import minetweaker.MineTweakerImplementationAPI;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;

/** Client-only, incrementally built source catalog. No scan simulation or reward packets. */
public final class ScanAllSources {

    private static final HashMap<Aspect, ArrayList<ItemStack>> SOURCES = new HashMap<>();
    private static final Set<Integer> SEEN = new HashSet<>();
    private static volatile boolean dirty = true;
    private static String playerName;
    private static Iterator<Map.Entry<Integer, ItemStack>> remaining;

    private ScanAllSources() {}

    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(new ScanAllSources());
        if (Loader.isModLoaded("MineTweaker3")) {
            ReloadHook.register();
        }
    }

    /** Keep optional MineTweaker event types out of the tick handler's reflected method signatures. */
    private static final class ReloadHook {

        private static void register() {
            MineTweakerImplementationAPI.onPostReload(event -> dirty = true);
        }
    }

    public static HashMap<Aspect, ArrayList<ItemStack>> sources(String player) {
        if (dirty || !player.equals(playerName)) {
            SOURCES.clear();
            SEEN.clear();
            remaining = null;
            playerName = player;
            dirty = false;
        }
        return SOURCES;
    }

    @SubscribeEvent
    public void disconnect(ClientDisconnectionFromServerEvent event) {
        dirty = true;
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getMinecraft();
        if (client.thePlayer == null || !ScanAll.itemsComplete(client.thePlayer.getCommandSenderName())) {
            if (playerName != null) {
                SOURCES.clear();
                SEEN.clear();
                remaining = null;
                playerName = null;
            }
            return;
        }
        sources(client.thePlayer.getCommandSenderName());
        if (remaining == null) {
            if (SEEN.size() == GuiResearchRecipe.cache.size()) return;
            remaining = GuiResearchRecipe.cache.entrySet()
                .iterator();
        }
        long deadline = System.nanoTime() + 4_000_000L;
        int processed = 0;
        while (remaining.hasNext() && processed++ < 64 && System.nanoTime() < deadline) {
            Map.Entry<Integer, ItemStack> entry = remaining.next();
            if (!SEEN.add(entry.getKey())) continue;
            ItemStack stack = entry.getValue();
            if (stack == null || stack.getItem() == null) continue;
            try {
                AspectList tags = ThaumcraftCraftingManager.getObjectTags(stack);
                tags = ThaumcraftCraftingManager.getBonusTags(stack, tags);
                if (tags == null) continue;
                for (Aspect aspect : tags.getAspects()) {
                    if (aspect == null || tags.getAmount(aspect) <= 0) continue;
                    ItemStack display = stack.copy();
                    display.stackSize = tags.getAmount(aspect);
                    SOURCES.computeIfAbsent(aspect, ignored -> new ArrayList<>())
                        .add(display);
                }
            } catch (RuntimeException | LinkageError | StackOverflowError failure) {
                ThaumicDabblery.LOG.warn("Could not index scanned item hash {}", entry.getKey(), failure);
            }
        }
        if (!remaining.hasNext()) remaining = null;
    }
}
