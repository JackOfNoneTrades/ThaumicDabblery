package org.fentanylsolutions.thaumicdabblery.feature.customaspects;

import java.util.ArrayList;
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
    private static boolean frozen;

    private CustomAspectRegistry() {}

    public static void registerAll(List<Definition> definitions) {
        if (frozen) {
            throw new IllegalStateException("Custom aspects are already registered; restart Minecraft to change them");
        }
        // Validate the entire graph before constructing anything: Aspect's constructor registers immediately.
        List<Definition> ordered = validate(definitions);
        for (Definition definition : ordered) {
            Aspect aspect = new ScriptedAspect(definition);
            OWNED.put(definition.tag, aspect);
        }
        frozen = true;
    }

    /** Non-mutating validation, also used by production probes. Forward references are resolved topologically. */
    public static List<Definition> validate(List<Definition> definitions) {
        Map<String, Definition> pending = new LinkedHashMap<>();
        Map<String, String> combinations = new HashMap<>();
        for (Aspect aspect : Aspect.aspects.values()) {
            Aspect[] components = aspect.getComponents();
            if (components != null && components.length == 2 && components[0] != null && components[1] != null) {
                combinations.put(pair(components[0].getTag(), components[1].getTag()), aspect.getTag());
            }
        }
        for (Definition definition : definitions) {
            if (Aspect.getAspect(definition.tag) != null || pending.put(definition.tag, definition) != null) {
                throw new IllegalArgumentException("Aspect ID already registered or defined twice: " + definition.tag);
            }
            String previous = combinations.put(pair(definition.first, definition.second), definition.tag);
            if (previous != null) {
                throw new IllegalArgumentException(
                    "Aspect " + definition.tag
                        + " uses the same component pair as "
                        + previous
                        + ": "
                        + definition.first
                        + " + "
                        + definition.second);
            }
        }

        List<Definition> ordered = new ArrayList<>();
        Set<String> visiting = new LinkedHashSet<>();
        Set<String> visited = new LinkedHashSet<>();
        for (String tag : pending.keySet()) {
            visit(tag, pending, visiting, visited, ordered);
        }
        return ordered;
    }

    private static void visit(String tag, Map<String, Definition> pending, Set<String> visiting, Set<String> visited,
        List<Definition> ordered) {
        if (visited.contains(tag) || Aspect.getAspect(tag) != null) {
            return;
        }
        Definition definition = pending.get(tag);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown component aspect " + tag + " referenced by " + visiting);
        }
        if (!visiting.add(tag)) {
            throw new IllegalArgumentException("Cyclic aspect components: " + visiting + " -> " + tag);
        }
        visit(definition.first, pending, visiting, visited, ordered);
        visit(definition.second, pending, visiting, visited, ordered);
        visiting.remove(tag);
        visited.add(tag);
        ordered.add(definition);
    }

    /** Detect addons replacing our objects or registering ambiguous combinations later in initialization. */
    public static void verifyRegistrations() {
        for (Map.Entry<String, Aspect> entry : OWNED.entrySet()) {
            Aspect owned = entry.getValue();
            if (Aspect.getAspect(entry.getKey()) != owned) {
                throw new IllegalStateException("Another mod replaced custom aspect " + entry.getKey());
            }
            Aspect[] components = owned.getComponents();
            String combination = pair(components[0].getTag(), components[1].getTag());
            for (Aspect aspect : Aspect.aspects.values()) {
                Aspect[] other = aspect.getComponents();
                if (aspect != owned && other != null
                    && other.length == 2
                    && other[0] != null
                    && other[1] != null
                    && combination.equals(pair(other[0].getTag(), other[1].getTag()))) {
                    throw new IllegalStateException(
                        "Custom aspect " + entry.getKey() + " conflicts with later aspect " + aspect.getTag());
                }
            }
        }
    }

    private static String pair(String first, String second) {
        return first.compareTo(second) <= 0 ? first + ":" + second : second + ":" + first;
    }

    public static final class Definition {

        public final String tag;
        public final int color;
        public final ResourceLocation image;
        public final String first;
        public final String second;
        public final String description;

        public Definition(String tag, int color, String image, String first, String second, String description) {
            this.tag = requireTag(tag);
            this.first = requireTag(first);
            this.second = requireTag(second);
            if (color < 0 || color > 0xFFFFFF) {
                throw new IllegalArgumentException("Aspect color must be an RGB integer from 0x000000 to 0xFFFFFF");
            }
            if (image == null || !image.matches("[a-z0-9_.-]+:[a-z0-9_][a-z0-9_./-]*\\.png") || image.contains("..")) {
                throw new IllegalArgumentException(
                    "Aspect icon must be a resource location such as pack:textures/aspects/time.png");
            }
            if (description == null || description.trim()
                .isEmpty()) {
                throw new IllegalArgumentException("Aspect description cannot be empty");
            }
            this.color = color;
            this.image = new ResourceLocation(image);
            this.description = description;
        }

        private static String requireTag(String tag) {
            if (tag == null || !tag.toLowerCase(Locale.ROOT)
                .matches("[a-z][a-z0-9_]{0,63}")) {
                throw new IllegalArgumentException(
                    "Aspect IDs must be 1-64 letters, digits or underscores, starting with a letter: " + tag);
            }
            return tag.toLowerCase(Locale.ROOT);
        }
    }

    private static final class ScriptedAspect extends Aspect {

        private final String description;

        private ScriptedAspect(Definition definition) {
            super(
                definition.tag,
                definition.color,
                new Aspect[] { Aspect.getAspect(definition.first), Aspect.getAspect(definition.second) },
                definition.image,
                1);
            description = definition.description;
        }

        @Override
        public String getLocalizedDescription() {
            return StatCollector.canTranslate("tc.aspect." + getTag()) ? super.getLocalizedDescription() : description;
        }
    }
}
