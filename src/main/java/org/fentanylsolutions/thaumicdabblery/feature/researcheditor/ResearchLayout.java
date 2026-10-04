package org.fentanylsolutions.thaumicdabblery.feature.researcheditor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.MoveResearchCompat;
import org.fentanylsolutions.thaumicdabblery.compat.tc4tweaks.AuraResearchCacheCompat;

import cpw.mods.fml.relauncher.ReflectionHelper;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;

/** A complete editable projection. Pages, recipe requirements and player knowledge are never changed. */
public final class ResearchLayout {

    public static final String[] FLAGS = { "Lost", "Hidden", "Secondary", "Round", "Spiky", "Virtual", "AutoUnlock" };
    private static final String[] FIELDS = { "isLost", "isHidden", "isSecondary", "isRound", "isSpecial", "isVirtual",
        "isAutoUnlock" };
    public final Map<String, Entry> entries = new LinkedHashMap<>();

    public static final class Entry {

        public final ResearchItem research;
        public String tab;
        public int x, y, flags;
        public Integer warp;
        public String[] parents, hidden, siblings;
        public boolean deleted;

        private Entry(ResearchItem item) {
            research = item;
            tab = item.category;
            x = item.displayColumn;
            y = item.displayRow;
            warp = warpMap().get(item.key);
            // Addon getters may depend on a client player. Capture the stored flags we also restore in apply().
            for (int i = 0; i < FIELDS.length; i++)
                if (Boolean.TRUE.equals(ReflectionHelper.getPrivateValue(ResearchItem.class, item, FIELDS[i])))
                    flags |= 1 << i;
            parents = copy(item.parents);
            hidden = copy(item.parentsHidden);
            siblings = copy(item.siblings);
        }

        private Entry(Entry source) {
            research = source.research;
            tab = source.tab;
            x = source.x;
            y = source.y;
            flags = source.flags;
            warp = source.warp;
            parents = copy(source.parents);
            hidden = copy(source.hidden);
            siblings = copy(source.siblings);
            deleted = source.deleted;
        }

        public boolean hasFlag(int index) {
            return (flags & (1 << index)) != 0;
        }

        public boolean isVirtual() {
            return hasFlag(5);
        }

        public int getWarp() {
            return warp == null ? 0 : warp;
        }
    }

    public static ResearchLayout capture() {
        ResearchLayout layout = new ResearchLayout();
        for (ResearchCategoryList category : ResearchCategories.researchCategories.values()) {
            for (ResearchItem research : category.research.values())
                layout.entries.put(research.key, new Entry(research));
        }
        return layout;
    }

    public ResearchLayout copy() {
        ResearchLayout result = new ResearchLayout();
        for (Map.Entry<String, Entry> entry : entries.entrySet())
            result.entries.put(entry.getKey(), new Entry(entry.getValue()));
        return result;
    }

    public Entry require(String key) {
        Entry entry = entries.get(key);
        if (entry == null || entry.deleted) throw new IllegalArgumentException("Missing research: " + key);
        return entry;
    }

    public boolean occupied(String key, String tab, int x, int y) {
        Entry moving = require(key);
        if (moving.isVirtual()) return false;
        for (Map.Entry<String, Entry> pair : entries.entrySet()) {
            Entry entry = pair.getValue();
            if (!pair.getKey()
                .equals(key) && !entry.deleted
                && !entry.isVirtual()
                && entry.tab.equals(tab)
                && entry.x == x
                && entry.y == y) return true;
        }
        return false;
    }

    public void move(String key, String tab, int x, int y) {
        if (!ResearchCategories.researchCategories.containsKey(tab))
            throw new IllegalArgumentException("Missing tab: " + tab);
        if (Math.abs((long) x) > 10000 || Math.abs((long) y) > 10000)
            throw new IllegalArgumentException("Position must be within -10000 to 10000");
        if (occupied(key, tab, x, y))
            throw new IllegalArgumentException("Position occupied. Use Swap positions instead.");
        Entry entry = require(key);
        entry.tab = tab;
        entry.x = x;
        entry.y = y;
    }

    public void moveToTab(String key, String tab) {
        if (!ResearchCategories.researchCategories.containsKey(tab))
            throw new IllegalArgumentException("Missing tab: " + tab);
        Entry entry = require(key);
        int originX = Math.max(-10000, Math.min(10000, entry.x));
        int originY = Math.max(-10000, Math.min(10000, entry.y));
        for (int radius = 0; radius <= 1000; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) != radius && Math.abs(dy) != radius) continue;
                    int x = originX + dx, y = originY + dy;
                    if (Math.abs(x) > 10000 || Math.abs(y) > 10000) continue;
                    if (!occupied(key, tab, x, y)) {
                        move(key, tab, x, y);
                        return;
                    }
                }
            }
        }
        throw new IllegalArgumentException("No free position in this tab");
    }

    public void swap(String first, String second) {
        Entry a = require(first), b = require(second);
        String tab = a.tab;
        int x = a.x, y = a.y;
        a.tab = b.tab;
        a.x = b.x;
        a.y = b.y;
        b.tab = tab;
        b.x = x;
        b.y = y;
    }

    public void toggleFlag(String key, int index) {
        Entry entry = require(key);
        entry.flags ^= 1 << index;
        if (index == 5 && !entry.isVirtual()) moveToTab(key, entry.tab);
    }

    public void setWarp(String key, int amount) {
        if (amount < 0) throw new IllegalArgumentException("Forbidden knowledge must be a non-negative whole number");
        require(key).warp = amount == 0 ? null : amount;
    }

    private static Map<Object, Integer> warpMap() {
        return ReflectionHelper.getPrivateValue(ThaumcraftApi.class, null, "warpMap");
    }

    public void parent(String child, String previous, String parent, boolean hiddenLink) {
        Entry entry = require(child);
        if (parent != null) {
            require(parent);
            if (child.equals(parent)) throw new IllegalArgumentException("A research cannot be its own parent");
            if (!Objects.equals(previous, parent)
                && (contains(entry.parents, parent) || contains(entry.hidden, parent)))
                throw new IllegalArgumentException("That parent is already linked");
            if (!contains(entry.parents, parent) && !contains(entry.hidden, parent) && reaches(parent, child))
                throw new IllegalArgumentException("That parent would create a prerequisite cycle");
        }
        if (previous != null) {
            entry.parents = without(entry.parents, previous);
            entry.hidden = without(entry.hidden, previous);
        }
        if (parent != null) {
            if (hiddenLink) entry.hidden = append(entry.hidden, parent);
            else entry.parents = append(entry.parents, parent);
        }
    }

    private boolean reaches(String from, String target) {
        Set<String> seen = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        pending.add(from);
        while (!pending.isEmpty()) {
            String key = pending.removeFirst();
            if (target.equals(key)) return true;
            Entry entry = entries.get(key);
            if (!seen.add(key) || entry == null || entry.deleted) continue;
            for (String link : links(entry.parents, entry.hidden)) if (link != null) pending.add(link);
        }
        return false;
    }

    public int delete(String key) {
        require(key).deleted = true;
        int count = 0;
        for (Entry entry : entries.values()) {
            for (String link : links(entry.parents, entry.hidden, entry.siblings)) if (key.equals(link)) count++;
            entry.parents = without(entry.parents, key);
            entry.hidden = without(entry.hidden, key);
            entry.siblings = without(entry.siblings, key);
        }
        return count;
    }

    /** Validate changed constraints only; mod packs may already contain overlaps or dependency cycles. */
    public void validate(ResearchLayout before) {
        for (Map.Entry<String, Entry> pair : entries.entrySet()) {
            String key = pair.getKey();
            Entry entry = pair.getValue(), original = before.entries.get(key);
            if (entry.deleted) continue;
            if (!ResearchCategories.researchCategories.containsKey(entry.tab))
                throw new IllegalArgumentException("Missing tab: " + entry.tab);
            if (original == null || !entry.tab.equals(original.tab)
                || entry.x != original.x
                || entry.y != original.y
                || original.isVirtual() && !entry.isVirtual()) {
                if (Math.abs((long) entry.x) > 10000 || Math.abs((long) entry.y) > 10000)
                    throw new IllegalArgumentException("Position out of range: " + key);
                if (occupied(key, entry.tab, entry.x, entry.y))
                    throw new IllegalArgumentException("Occupied destination: " + key);
            }
            for (String parent : links(entry.parents, entry.hidden)) {
                if (original != null && (contains(original.parents, parent) || contains(original.hidden, parent)))
                    continue;
                require(parent);
                if (key.equals(parent) || reaches(parent, key))
                    throw new IllegalArgumentException("Prerequisite cycle: " + key + " -> " + parent);
            }
        }
    }

    /** All placements happen together so swapping occupied positions needs no temporary research coordinates. */
    public void apply() {
        Map<Object, Integer> warp = warpMap();
        for (ResearchCategoryList category : ResearchCategories.researchCategories.values()) {
            for (String key : entries.keySet()) category.research.remove(key);
        }
        for (Map.Entry<String, Entry> pair : entries.entrySet()) {
            Entry entry = pair.getValue();
            ResearchItem research = entry.research;
            if (entry.deleted || entry.warp == null) warp.remove(pair.getKey());
            else warp.put(pair.getKey(), entry.warp);
            MoveResearchCompat.setPositionAndCategory(research, entry.x, entry.y, entry.tab);
            research.parents = copy(entry.parents);
            research.parentsHidden = copy(entry.hidden);
            research.siblings = copy(entry.siblings);
            for (int i = 0; i < FIELDS.length; i++)
                ReflectionHelper.setPrivateValue(ResearchItem.class, research, entry.hasFlag(i), FIELDS[i]);
            ResearchCategoryList category = ResearchCategories.getResearchList(entry.tab);
            if (!entry.deleted && category != null) category.research.put(pair.getKey(), research);
            AuraResearchCacheCompat.update(pair.getKey(), entry.deleted ? null : research);
        }
        for (ResearchCategoryList category : ResearchCategories.researchCategories.values()) {
            category.minDisplayColumn = category.maxDisplayColumn = category.minDisplayRow = category.maxDisplayRow = 0;
            for (ResearchItem research : category.research.values()) {
                category.minDisplayColumn = Math.min(category.minDisplayColumn, research.displayColumn);
                category.maxDisplayColumn = Math.max(category.maxDisplayColumn, research.displayColumn);
                category.minDisplayRow = Math.min(category.minDisplayRow, research.displayRow);
                category.maxDisplayRow = Math.max(category.maxDisplayRow, research.displayRow);
            }
        }
        Map<?, ?> cache = ReflectionHelper.getPrivateValue(ThaumcraftApi.class, null, "keyCache");
        cache.clear();
    }

    public String script(ResearchLayout baseline) {
        StringBuilder script = new StringBuilder(
            "// Managed by the Thaumonomicon editor. Changes apply after ordinary scripts.\n"
                + "// Close the editor and reload scripts after editing this file by hand.\n"
                + "import mods.thaumicdabblery.ResearchEditor;\n\n");
        ResearchLayout base = baseline.copy();
        // Deletions already detach all incoming links; do not serialize those incidental changes again.
        for (Map.Entry<String, Entry> pair : new TreeMap<>(entries).entrySet()) {
            Entry original = base.entries.get(pair.getKey());
            if (pair.getValue().deleted && original != null && !original.deleted) {
                script.append("ResearchEditor.remove(")
                    .append(quote(pair.getKey()))
                    .append(");\n");
                base.delete(pair.getKey());
            }
        }
        for (Map.Entry<String, Entry> pair : new TreeMap<>(entries).entrySet()) {
            String key = pair.getKey();
            Entry entry = pair.getValue(), original = base.entries.get(key);
            if (entry.deleted || original == null) continue;
            if (!entry.tab.equals(original.tab) || entry.x != original.x || entry.y != original.y)
                script.append("ResearchEditor.move(")
                    .append(quote(key))
                    .append(", ")
                    .append(quote(entry.tab))
                    .append(", ")
                    .append(entry.x)
                    .append(", ")
                    .append(entry.y)
                    .append(");\n");
            for (int i = 0; i < FLAGS.length; i++)
                if (entry.hasFlag(i) != original.hasFlag(i)) script.append("ResearchEditor.flag(")
                    .append(quote(key))
                    .append(", ")
                    .append(quote(FLAGS[i]))
                    .append(", ")
                    .append(entry.hasFlag(i))
                    .append(");\n");
            if (entry.getWarp() != original.getWarp()) script.append("ResearchEditor.warp(")
                .append(quote(key))
                .append(", ")
                .append(entry.getWarp())
                .append(");\n");
            if (!Arrays.equals(entry.parents, original.parents) || !Arrays.equals(entry.hidden, original.hidden))
                script.append("ResearchEditor.parents(")
                    .append(quote(key))
                    .append(", ")
                    .append(array(entry.parents))
                    .append(", ")
                    .append(array(entry.hidden))
                    .append(");\n");
        }
        return script.toString();
    }

    public static String quote(String value) {
        if (value == null) return "null";
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == '"') out.append('\\')
                .append(c);
            else if (c == '\n') out.append("\\n");
            else if (c == '\r') out.append("\\r");
            else if (c == '\t') out.append("\\t");
            else out.append(c);
        }
        return out.append('"')
            .toString();
    }

    private static String array(String[] values) {
        if (values == null) return "null";
        StringBuilder result = new StringBuilder("[");
        for (String value : values) {
            if (result.length() > 1) result.append(", ");
            result.append(quote(value));
        }
        return result.append(']')
            .toString();
    }

    public static List<String> links(String[]... arrays) {
        List<String> result = new ArrayList<>();
        for (String[] array : arrays) if (array != null) result.addAll(Arrays.asList(array));
        return result;
    }

    private static boolean contains(String[] array, String key) {
        return array != null && Arrays.asList(array)
            .contains(key);
    }

    private static String[] copy(String[] array) {
        return array == null ? null : array.clone();
    }

    private static String[] without(String[] array, String key) {
        if (!contains(array, key)) return copy(array);
        List<String> result = new ArrayList<>();
        for (String value : array) if (!Objects.equals(value, key)) result.add(value);
        return result.toArray(new String[0]);
    }

    private static String[] append(String[] array, String key) {
        List<String> result = links(array);
        result.add(key);
        return result.toArray(new String[0]);
    }
}
