package org.fentanylsolutions.thaumicdabblery.feature.customaspects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import thaumcraft.api.aspects.Aspect;

/** Startup registrations deliberately never enter MineTweaker's undo queue. */
public final class CustomAspectRegistry {

    private static final Map<String, Aspect> OWNED = new LinkedHashMap<>();
    private static final Map<String, String[]> EXPECTED = new LinkedHashMap<>();
    private static final Set<Aspect> HIDDEN = new LinkedHashSet<>();
    private static final Set<Aspect> VISIBLE_PRIMALS = new LinkedHashSet<>();
    private static boolean frozen;

    private CustomAspectRegistry() {}

    public static boolean isCustomPrimal(Aspect aspect) {
        return aspect != null && aspect.isPrimal() && OWNED.get(aspect.getTag()) == aspect;
    }

    public static boolean hasChanges() {
        return !OWNED.isEmpty();
    }

    public static boolean isHidden(Aspect aspect) {
        return HIDDEN.contains(aspect);
    }

    public static Set<Aspect> visiblePrimals() {
        return Collections.unmodifiableSet(VISIBLE_PRIMALS);
    }

    public static void registerAll(List<Definition> definitions) {
        registerAll(definitions, Collections.emptyList());
    }

    public static void registerAll(List<Definition> definitions, List<ComponentEdit> edits) {
        if (frozen)
            throw new IllegalStateException("Custom aspects are already registered; restart Minecraft to change them");
        validate(definitions, edits);
        // Constructors register immediately. Validate the whole batch first, then wire references only after all
        // new objects exist, retaining the identities of existing aspects referenced by recipes and addons.
        for (Definition definition : definitions) {
            Aspect aspect = new ScriptedAspect(definition);
            OWNED.put(definition.tag, aspect);
            EXPECTED.put(definition.tag, definition.components());
            if (definition.hiddenUntilScanned) HIDDEN.add(aspect);
            else if (definition.first == null) VISIBLE_PRIMALS.add(aspect);
        }
        for (ComponentEdit edit : edits) {
            OWNED.put(edit.tag, Aspect.getAspect(edit.tag));
            EXPECTED.put(edit.tag, new String[] { edit.first, edit.second });
        }
        for (Map.Entry<String, String[]> entry : EXPECTED.entrySet()) {
            String[] pair = entry.getValue();
            OWNED.get(entry.getKey())
                .setComponents(
                    pair == null ? null : new Aspect[] { Aspect.getAspect(pair[0]), Aspect.getAspect(pair[1]) });
        }
        frozen = true;
    }

    public static List<Definition> validate(List<Definition> definitions) {
        return validate(definitions, Collections.emptyList());
    }

    /** Validate the final graph, including edits through existing aspects, without modifying any object. */
    public static List<Definition> validate(List<Definition> definitions, List<ComponentEdit> edits) {
        Map<String, String[]> graph = new LinkedHashMap<>();
        for (Aspect aspect : Aspect.aspects.values()) graph.put(aspect.getTag(), components(aspect));
        Map<String, Definition> pending = new LinkedHashMap<>();
        Set<String> managed = new LinkedHashSet<>();
        for (Definition definition : definitions) {
            if (graph.containsKey(definition.tag) || pending.put(definition.tag, definition) != null)
                throw new IllegalArgumentException("Aspect ID already registered or defined twice: " + definition.tag);
            graph.put(definition.tag, definition.components());
            managed.add(definition.tag);
        }
        Set<String> edited = new LinkedHashSet<>();
        for (ComponentEdit edit : edits) {
            if (!graph.containsKey(edit.tag)) throw new IllegalArgumentException("Unknown aspect to edit: " + edit.tag);
            if (graph.get(edit.tag) == null)
                throw new IllegalArgumentException("Cannot change a primal's components: " + edit.tag);
            if (!edited.add(edit.tag))
                throw new IllegalArgumentException("Aspect components edited twice: " + edit.tag);
            graph.put(edit.tag, new String[] { edit.first, edit.second });
            managed.add(edit.tag);
        }
        if (graph.values()
            .stream()
            .filter(value -> value == null)
            .count() > 128)
            throw new IllegalArgumentException("Thaumcraft's vis packets support at most 128 primal aspects in total");
        validatePairs(graph, managed);
        List<Definition> ordered = new ArrayList<>();
        Set<String> visiting = new LinkedHashSet<>();
        Set<String> visited = new LinkedHashSet<>();
        for (String tag : managed) visit(tag, graph, pending, visiting, visited, ordered);
        return ordered;
    }

    private static void validatePairs(Map<String, String[]> graph, Set<String> managed) {
        Map<String, String> combinations = new HashMap<>();
        for (Map.Entry<String, String[]> entry : graph.entrySet()) {
            String[] components = entry.getValue();
            if (components == null) continue;
            String previous = combinations.put(pair(components[0], components[1]), entry.getKey());
            if (previous != null && (managed.contains(previous) || managed.contains(entry.getKey())))
                throw new IllegalArgumentException(
                    "Aspect " + entry.getKey() + " uses the same component pair as " + previous);
        }
    }

    private static void visit(String tag, Map<String, String[]> graph, Map<String, Definition> pending,
        Set<String> visiting, Set<String> visited, List<Definition> ordered) {
        if (visited.contains(tag)) return;
        if (!graph.containsKey(tag))
            throw new IllegalArgumentException("Unknown component aspect " + tag + " referenced by " + visiting);
        if (!visiting.add(tag))
            throw new IllegalArgumentException("Cyclic aspect components: " + visiting + " -> " + tag);
        if (visiting.size() > 256)
            throw new IllegalArgumentException("Aspect component graph exceeds 256 levels: " + tag);
        String[] components = graph.get(tag);
        if (components != null) {
            visit(components[0], graph, pending, visiting, visited, ordered);
            visit(components[1], graph, pending, visiting, visited, ordered);
        }
        visiting.remove(tag);
        visited.add(tag);
        if (pending.containsKey(tag)) ordered.add(pending.get(tag));
    }

    /** Detect later addon mutations and ambiguous combinations after initialization and script reloads. */
    public static void verifyRegistrations() {
        if (OWNED.isEmpty()) return;
        Map<String, String[]> graph = new LinkedHashMap<>();
        for (Aspect aspect : Aspect.aspects.values()) graph.put(aspect.getTag(), components(aspect));
        for (Map.Entry<String, Aspect> entry : OWNED.entrySet()) {
            if (Aspect.getAspect(entry.getKey()) != entry.getValue()
                || !Arrays.equals(EXPECTED.get(entry.getKey()), graph.get(entry.getKey())))
                throw new IllegalStateException("Another mod changed managed aspect " + entry.getKey());
        }
        validatePairs(graph, OWNED.keySet());
        Set<String> visited = new LinkedHashSet<>();
        for (String tag : OWNED.keySet())
            visit(tag, graph, Collections.emptyMap(), new LinkedHashSet<>(), visited, new ArrayList<>());
    }

    private static String[] components(Aspect aspect) {
        Aspect[] components = aspect.getComponents();
        if (components == null) return null;
        if (components.length != 2 || components[0] == null || components[1] == null)
            throw new IllegalArgumentException("Malformed components for aspect " + aspect.getTag());
        return new String[] { components[0].getTag(), components[1].getTag() };
    }

    private static String pair(String first, String second) {
        return first.compareTo(second) <= 0 ? first + ":" + second : second + ":" + first;
    }

    private static String requireTag(String tag) {
        if (tag == null || !tag.toLowerCase(Locale.ROOT)
            .matches("[a-z][a-z0-9_]{0,63}"))
            throw new IllegalArgumentException(
                "Aspect IDs must be 1-64 letters, digits or underscores, starting with a letter: " + tag);
        return tag.toLowerCase(Locale.ROOT);
    }

    public static final class ComponentEdit {

        public final String tag, first, second;

        public ComponentEdit(String tag, String first, String second) {
            this.tag = requireTag(tag);
            this.first = requireTag(first);
            this.second = requireTag(second);
        }
    }

    public static final class Definition {

        public final String tag;
        public final int color;
        public final ResourceLocation image;
        public final String first, second, description;
        public final boolean hiddenUntilScanned;

        public Definition(String tag, int color, String image, String first, String second, String description) {
            this(tag, color, image, requireTag(first), requireTag(second), description, false);
        }

        public Definition(String tag, int color, String image, String description, boolean hiddenUntilScanned) {
            this(tag, color, image, null, null, description, hiddenUntilScanned);
        }

        public Definition(String tag, int color, String image, String first, String second, String description,
            boolean hiddenUntilScanned) {
            this.tag = requireTag(tag);
            this.first = first == null && second == null ? null : requireTag(first);
            this.second = first == null && second == null ? null : requireTag(second);
            if (color < 0 || color > 0xFFFFFF)
                throw new IllegalArgumentException("Aspect color must be an RGB integer from 0x000000 to 0xFFFFFF");
            if (image == null || !image.matches("[a-z0-9_.-]+:[a-z0-9_][a-z0-9_./-]*\\.png") || image.contains(".."))
                throw new IllegalArgumentException(
                    "Aspect icon must be a resource location such as pack:textures/aspects/time.png");
            if (description == null || description.trim()
                .isEmpty()) throw new IllegalArgumentException("Aspect description cannot be empty");
            this.color = color;
            this.image = new ResourceLocation(image);
            this.description = description;
            this.hiddenUntilScanned = hiddenUntilScanned;
        }

        private String[] components() {
            return first == null ? null : new String[] { first, second };
        }
    }

    private static final class ScriptedAspect extends Aspect {

        private final String description;

        private ScriptedAspect(Definition definition) {
            super(definition.tag, definition.color, null, definition.image, 1);
            description = definition.description;
            setChatcolor("f");
        }

        @Override
        public String getLocalizedDescription() {
            return StatCollector.canTranslate("tc.aspect." + getTag()) ? super.getLocalizedDescription() : description;
        }
    }
}
