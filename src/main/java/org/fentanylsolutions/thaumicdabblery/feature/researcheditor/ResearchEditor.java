package org.fentanylsolutions.thaumicdabblery.feature.researcheditor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

import cpw.mods.fml.relauncher.FMLInjectionData;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.thaumicdabblery.ResearchEditor")
public final class ResearchEditor {

    public static final String FILE_NAME = "thaumicdabblery_research_editor.zs";
    private static final List<Consumer<ResearchLayout>> SCRIPT_PATCHES = new ArrayList<>(),
        EDITOR_PATCHES = new ArrayList<>();
    private static final Deque<Change> UNDO = new ArrayDeque<>(), REDO = new ArrayDeque<>();
    private static ResearchLayout baseline, current;
    private static EditorFile file;
    private static String problem;
    private static int generation;
    private static boolean collecting;
    private static final Set<String> REMOVED_RESEARCH = new HashSet<>();
    private static final List<TabRemoval> TAB_REMOVALS = new ArrayList<>();
    private static final List<RetiredTab> RETIRED_TABS = new ArrayList<>();

    private ResearchEditor() {}

    public static void register() {
        MineTweakerAPI.registerClass(ResearchEditor.class);
        MineTweakerImplementationAPI.onReloadEvent(event -> prepareReload());
        MineTweakerImplementationAPI.onPostReload(event -> finishReload());
    }

    private static synchronized void prepareReload() {
        collecting = true;
        REMOVED_RESEARCH.clear();
        TAB_REMOVALS.clear();
        RETIRED_TABS.clear();
        SCRIPT_PATCHES.clear();
        EDITOR_PATCHES.clear();
        baseline = current = null;
        UNDO.clear();
        REDO.clear();
        generation++;
        problem = null;
        try {
            File directory = (File) FMLInjectionData.data()[6];
            file = new EditorFile(new File(new File(directory, "scripts"), FILE_NAME).toPath());
        } catch (IOException exception) {
            file = null;
            ThaumicDabblery.LOG.error("Could not read Thaumonomicon editor script " + FILE_NAME, exception);
            problem = EditorFile.failureMessage(exception);
        }
    }

    private static synchronized void finishReload() {
        collecting = false;
        ResearchLayout original = ResearchLayout.capture();
        baseline = original.copy();
        // Newer replacements win when a key was reused across several tab generations.
        for (int i = RETIRED_TABS.size() - 1; i >= 0; i--) {
            RetiredTab retired = RETIRED_TABS.get(i);
            baseline.includeRetiredTab(retired.before, retired.tab, false, REMOVED_RESEARCH);
            original.includeRetiredTab(retired.before, retired.tab, true, REMOVED_RESEARCH);
        }
        baseline.detachRemovedReferences(REMOVED_RESEARCH);
        ResearchLayout unpatched = baseline.copy();
        try {
            for (Consumer<ResearchLayout> patch : SCRIPT_PATCHES) patch.accept(baseline);
            baseline.finishTabRemovals()
                .validate(original);
        } catch (IllegalArgumentException exception) {
            problem = exception.getMessage();
            MineTweakerAPI.logError("Research editor script baseline rejected: " + problem);
            baseline = unpatched;
            current = baseline.finishTabRemovals();
            MineTweakerAPI.apply(new Overlay(original, current));
            return;
        }
        current = baseline.copy();
        try {
            for (Consumer<ResearchLayout> patch : EDITOR_PATCHES) patch.accept(current);
            current = current.finishTabRemovals();
            current.validate(original);
        } catch (IllegalArgumentException exception) {
            problem = exception.getMessage();
            MineTweakerAPI.logError("Research editor overlay rejected: " + problem);
            current = baseline.finishTabRemovals();
        }
        // Restore both layers BEFORE ordinary script actions are undone on the next reload.
        MineTweakerAPI.apply(new Overlay(original, current));
    }

    private static List<Consumer<ResearchLayout>> patches() {
        // ZenModule records the script group's original filename on __ZenMain__.run, including in production.
        // Use the executing group, not a helper function's source file or an import/variable name.
        for (StackTraceElement frame : Thread.currentThread()
            .getStackTrace()) {
            if ("__ZenMain__".equals(frame.getClassName()) && "run".equals(frame.getMethodName()))
                return FILE_NAME.equals(frame.getFileName()) ? EDITOR_PATCHES : SCRIPT_PATCHES;
        }
        return SCRIPT_PATCHES;
    }

    @ZenMethod
    public static synchronized void move(String key, String tab, int x, int y) {
        researchPatch(key, layout -> {
            ResearchLayout.Entry entry = layout.require(key);
            entry.tab = tab;
            entry.x = x;
            entry.y = y;
            entry.moved = true;
        });
    }

    @ZenMethod
    public static synchronized void flag(String key, String flag, boolean value) {
        researchPatch(key, layout -> {
            ResearchLayout.Entry entry = layout.require(key);
            for (int i = 0; i < ResearchLayout.FLAGS.length; i++) {
                if (ResearchLayout.FLAGS[i].equalsIgnoreCase(flag)) {
                    entry.flags = value ? entry.flags | (1 << i) : entry.flags & ~(1 << i);
                    return;
                }
            }
            throw new IllegalArgumentException("Unknown research flag: " + flag);
        });
    }

    @ZenMethod
    public static synchronized void warp(String key, int amount) {
        researchPatch(key, layout -> layout.setWarp(key, amount));
    }

    @ZenMethod
    public static synchronized void parents(String key, String[] visible, String[] hidden) {
        final String[] normalCopy = visible == null ? null : visible.clone();
        final String[] hiddenCopy = hidden == null ? null : hidden.clone();
        researchPatch(key, layout -> {
            ResearchLayout.Entry entry = layout.require(key);
            entry.parents = survivingParents(layout, normalCopy);
            entry.hidden = survivingParents(layout, hiddenCopy);
        });
    }

    @ZenMethod
    public static synchronized void remove(String key) {
        researchPatch(key, layout -> layout.delete(key));
    }

    /** Compatibility alias. Both public APIs now use the same removal action. */
    @ZenMethod
    public static synchronized void removeTab(String tab) {
        modtweaker2.mods.thaumcraft.handlers.Research.removeTab(tab);
    }

    /** Intercept ordinary script actions, but leave direct Java actions outside reload unchanged. */
    public static synchronized boolean deferTabRemoval(String tab) {
        if (!collecting) return false;
        TabRemoval removal = new TabRemoval(tab);
        TAB_REMOVALS.add(removal);
        patches().add(removal);
        return true;
    }

    /** Re-adding a pending tab starts a new generation; preserve old entries for deferred moves. */
    public static synchronized ResearchLayout prepareTabAddition(String tab) {
        if (!collecting) return null;
        boolean replacing = false;
        for (TabRemoval removal : TAB_REMOVALS) {
            if (removal.active && java.util.Objects.equals(tab, removal.tab)) {
                removal.active = false;
                replacing = true;
            }
        }
        if (!replacing) return null;
        ResearchLayout before = ResearchLayout.capture();
        // An existing key may have been removed and re-created earlier in this script.
        for (ResearchLayout.Entry entry : before.entries.values())
            if (java.util.Objects.equals(entry.tab, tab)) REMOVED_RESEARCH.remove(entry.research.key);
        RETIRED_TABS.add(new RetiredTab(tab, before));
        return before;
    }

    private static final class TabRemoval implements Consumer<ResearchLayout> {

        private final String tab;
        private boolean active = true;

        private TabRemoval(String tab) {
            this.tab = tab;
        }

        public void accept(ResearchLayout layout) {
            if (active && layout.hasTab(tab)) layout.removeTab(tab);
        }
    }

    private static final class RetiredTab {

        private final String tab;
        private final ResearchLayout before;

        private RetiredTab(String tab, ResearchLayout before) {
            this.tab = tab;
            this.before = before;
        }
    }

    /** Called only for existing entries actually removed by ordinary ModTweaker actions during this reload. */
    public static synchronized void recordRemovedResearch(String key) {
        if (collecting) REMOVED_RESEARCH.add(key);
    }

    private static void researchPatch(String key, Consumer<ResearchLayout> patch) {
        patches().add(layout -> {
            if (removedByScripts(layout, key)) {
                MineTweakerAPI
                    .logWarning("Skipping editor changes for research " + key + ": removed by ordinary scripts.");
                return;
            }
            patch.accept(layout);
        });
    }

    private static boolean removedByScripts(ResearchLayout layout, String key) {
        return REMOVED_RESEARCH.contains(key) && !layout.entries.containsKey(key);
    }

    private static String[] survivingParents(ResearchLayout layout, String[] parents) {
        if (parents == null) return null;
        List<String> result = new ArrayList<>();
        for (String parent : parents) {
            if (removedByScripts(layout, parent))
                MineTweakerAPI.logWarning("Skipping editor prerequisite " + parent + ": removed by ordinary scripts.");
            else result.add(parent);
        }
        return result.toArray(new String[0]);
    }

    public static synchronized int generation() {
        return generation;
    }

    public static synchronized ResearchLayout layout() {
        if (current == null) throw new IllegalStateException("Research scripts are still loading");
        return current.copy();
    }

    public static synchronized String problem() {
        return current == null ? "Research scripts are still loading" : problem;
    }

    public static synchronized String undoName() {
        return UNDO.isEmpty() ? null : UNDO.peek().name;
    }

    public static synchronized String redoName() {
        return REDO.isEmpty() ? null : REDO.peek().name;
    }

    public static synchronized void forgetHistory() {
        UNDO.clear();
        REDO.clear();
    }

    public static synchronized boolean edit(String name, Consumer<ResearchLayout> edit) throws IOException {
        ResearchLayout next = layout();
        edit.accept(next);
        next.validate(current);
        if (next.script(baseline)
            .equals(current.script(baseline))) return false;
        Change change = new Change(name, current, next);
        save(next);
        UNDO.push(change);
        if (UNDO.size() > 100) UNDO.removeLast();
        REDO.clear();
        return true;
    }

    public static synchronized void undo() throws IOException {
        if (UNDO.isEmpty()) return;
        Change change = UNDO.peek();
        save(change.before);
        REDO.push(UNDO.pop());
    }

    public static synchronized void redo() throws IOException {
        if (REDO.isEmpty()) return;
        Change change = REDO.peek();
        save(change.after);
        UNDO.push(REDO.pop());
    }

    private static void save(ResearchLayout next) throws IOException {
        if (problem != null) throw new IOException(problem);
        if (file == null) throw new IOException("Editor script file is unavailable");
        file.write(next.script(baseline));
        next.apply();
        current = next;
    }

    private static final class Change {

        private final String name;
        private final ResearchLayout before, after;

        private Change(String name, ResearchLayout before, ResearchLayout after) {
            this.name = name;
            this.before = before;
            this.after = after;
        }
    }

    private static final class Overlay implements IUndoableAction {

        private final ResearchLayout before, after;

        private Overlay(ResearchLayout before, ResearchLayout after) {
            this.before = before;
            this.after = after;
        }

        @Override
        public void apply() {
            after.apply();
        }

        @Override
        public void undo() {
            synchronized (ResearchEditor.class) {
                before.apply();
                current = baseline = null;
                UNDO.clear();
                REDO.clear();
                generation++;
            }
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public String describe() {
            return "Applying Thaumonomicon editor layout";
        }

        @Override
        public String describeUndo() {
            return "Restoring Thaumonomicon layout before editor changes";
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }
}
