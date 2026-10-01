package org.fentanylsolutions.thaumicdabblery.feature.researcheditor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import cpw.mods.fml.relauncher.FMLInjectionData;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.thaumicdabblery.ResearchEditor")
public final class ResearchEditor {

    public static final String FILE_NAME = "thaumicdabblery_research_editor.zs";
    private static final List<Consumer<ResearchLayout>> PATCHES = new ArrayList<>();
    private static final Deque<Change> UNDO = new ArrayDeque<>(), REDO = new ArrayDeque<>();
    private static ResearchLayout baseline, current;
    private static EditorFile file;
    private static String problem;
    private static int generation;

    private ResearchEditor() {}

    public static void register() {
        MineTweakerAPI.registerClass(ResearchEditor.class);
        MineTweakerImplementationAPI.onReloadEvent(event -> prepareReload());
        MineTweakerImplementationAPI.onPostReload(event -> finishReload());
    }

    private static synchronized void prepareReload() {
        PATCHES.clear();
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
            problem = exception.getMessage();
        }
    }

    private static synchronized void finishReload() {
        baseline = ResearchLayout.capture();
        current = baseline.copy();
        try {
            for (Consumer<ResearchLayout> patch : PATCHES) patch.accept(current);
            current.validate(baseline);
        } catch (IllegalArgumentException exception) {
            problem = exception.getMessage();
            MineTweakerAPI.logError("Research editor overlay rejected: " + problem);
            current = baseline.copy();
        }
        // The last action rolls the entire overlay back BEFORE ordinary script actions are undone.
        MineTweakerAPI.apply(new Overlay(baseline, current));
    }

    @ZenMethod
    public static synchronized void move(String key, String tab, int x, int y) {
        PATCHES.add(layout -> {
            ResearchLayout.Entry entry = layout.require(key);
            entry.tab = tab;
            entry.x = x;
            entry.y = y;
        });
    }

    @ZenMethod
    public static synchronized void flag(String key, String flag, boolean value) {
        PATCHES.add(layout -> {
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
    public static synchronized void parents(String key, String[] visible, String[] hidden) {
        final String[] normalCopy = visible == null ? null : visible.clone();
        final String[] hiddenCopy = hidden == null ? null : hidden.clone();
        PATCHES.add(layout -> {
            ResearchLayout.Entry entry = layout.require(key);
            entry.parents = normalCopy == null ? null : normalCopy.clone();
            entry.hidden = hiddenCopy == null ? null : hiddenCopy.clone();
        });
    }

    @ZenMethod
    public static synchronized void remove(String key) {
        PATCHES.add(layout -> layout.delete(key));
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
