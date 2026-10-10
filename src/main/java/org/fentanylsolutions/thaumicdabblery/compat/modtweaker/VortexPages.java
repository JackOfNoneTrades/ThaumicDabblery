package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.planarvortex.VortexPage;

import cpw.mods.fml.relauncher.ReflectionHelper;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;

public final class VortexPages {

    private static final List<Add> ACTIVE = new ArrayList<>();

    public static void add(String research, String key, ItemStack icon) {
        if (research == null || research.trim()
            .isEmpty()) throw new IllegalArgumentException("Research key must not be empty");
        ResearchItem owner = ResearchCategories.getResearch(research);
        if (owner == null) throw new IllegalArgumentException("Unknown research: " + research);
        if (VortexPage.resolve(key) == null)
            throw new IllegalArgumentException("Unknown or removed vortex recipe: " + key);
        if (owner.getPages() != null) for (ResearchPage page : owner.getPages()) {
            if (page instanceof VortexPage && ((VortexPage) page).key.equals(key))
                throw new IllegalArgumentException("Vortex recipe page already attached to " + research + ": " + key);
        }
        MineTweakerAPI.apply(new Add(owner, new VortexPage(key, icon)));
    }

    public static void refresh() {
        for (Add action : ACTIVE) action.page.refresh();
        if (!ACTIVE.isEmpty()) clearLinks();
    }

    private static void clearLinks() {
        Map<?, ?> cache = ReflectionHelper.getPrivateValue(ThaumcraftApi.class, null, "keyCache");
        cache.clear();
    }

    public static List<Detached> detach(String key) {
        List<Detached> removed = new ArrayList<>();
        for (Add action : ACTIVE) {
            if (!action.page.key.equals(key) || !action.attached()) continue;
            int index = Arrays.asList(action.owner.getPages())
                .indexOf(action.page);
            removed.add(new Detached(action, index));
            action.remove();
        }
        return removed;
    }

    public static void restore(List<Detached> removed) {
        if (removed == null) return;
        // Reverse order also preserves the positions of multiple removed pages in one research.
        for (int i = removed.size() - 1; i >= 0; i--) {
            Detached slot = removed.get(i);
            Add action = slot.action;
            if (ResearchCategories.getResearch(action.owner.key) != action.owner || action.attached()) continue;
            List<ResearchPage> pages = new ArrayList<>();
            if (action.owner.getPages() != null) pages.addAll(Arrays.asList(action.owner.getPages()));
            pages.add(Math.min(slot.index, pages.size()), action.page);
            action.owner.setPages(pages.toArray(new ResearchPage[0]));
        }
        refresh();
    }

    public static final class Detached {

        private final Add action;
        private final int index;

        private Detached(Add action, int index) {
            this.action = action;
            this.index = index;
        }
    }

    private static final class Add implements IUndoableAction {

        private final ResearchItem owner;
        private final VortexPage page;
        private ResearchPage[] before;

        private Add(ResearchItem owner, VortexPage page) {
            this.owner = owner;
            this.page = page;
        }

        public void apply() {
            before = owner.getPages();
            ResearchPage[] pages = before == null ? new ResearchPage[1] : Arrays.copyOf(before, before.length + 1);
            pages[pages.length - 1] = page;
            owner.setPages(pages);
            ACTIVE.add(this);
            clearLinks();
        }

        private boolean attached() {
            return ResearchCategories.getResearch(owner.key) == owner && owner.getPages() != null
                && Arrays.asList(owner.getPages())
                    .contains(page);
        }

        private void remove() {
            if (!attached()) return;
            List<ResearchPage> pages = new ArrayList<>(Arrays.asList(owner.getPages()));
            pages.remove(page);
            ResearchPage[] next = pages.toArray(new ResearchPage[0]);
            owner.setPages(Arrays.equals(next, before == null ? new ResearchPage[0] : before) ? before : next);
            clearLinks();
        }

        public void undo() {
            remove();
            ACTIVE.remove(this);
        }

        public boolean canUndo() {
            return true;
        }

        public String describe() {
            return "Add vortex page to " + owner.key;
        }

        public String describeUndo() {
            return "Remove vortex page from " + owner.key;
        }

        public Object getOverrideKey() {
            return null;
        }
    }
}
