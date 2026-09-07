package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;
import org.fentanylsolutions.thaumicdabblery.feature.customaspects.CustomAspectRegistry;
import org.fentanylsolutions.thaumicdabblery.feature.customaspects.CustomAspectRegistry.Definition;

import minetweaker.MineTweakerAPI;
import stanhebben.zenscript.IZenErrorLogger;
import stanhebben.zenscript.ZenModule;
import stanhebben.zenscript.ZenParsedFile;
import stanhebben.zenscript.ZenTokener;
import stanhebben.zenscript.compiler.IEnvironmentGlobal;
import stanhebben.zenscript.impl.GenericCompileEnvironment;
import stanhebben.zenscript.impl.GenericRegistry;
import stanhebben.zenscript.util.ZenPosition;

/** An isolated early ZenScript pass: it exposes definitions, not runtime recipes, brackets or undoable actions. */
public final class CustomAspectScriptLoader {

    private static List<Definition> collecting;
    private static boolean loaded;

    private CustomAspectScriptLoader() {}

    static void queue(Definition definition) {
        if (collecting == null) {
            throw new IllegalStateException(
                "CustomAspects.register is startup-only: put definitions in config/thaumicdabblery/aspects/*.zs and restart Minecraft");
        }
        collecting.add(definition);
    }

    public static void load(Path directory) {
        if (loaded) {
            throw new IllegalStateException("Custom aspect scripts can only run once at startup");
        }
        loaded = true;
        try {
            Files.createDirectories(directory);
            List<Path> paths;
            try (Stream<Path> files = Files.list(directory)) {
                paths = files.filter(Files::isRegularFile)
                    .filter(
                        path -> path.getFileName()
                            .toString()
                            .endsWith(".zs"))
                    .sorted()
                    .collect(Collectors.toList());
            }
            List<Definition> definitions = compileAndCollect(paths);
            CustomAspectRegistry.registerAll(definitions);
            ThaumicDabblery.LOG.info("Registered {} custom aspects from {}", definitions.size(), directory);
            MineTweakerAPI.logInfo("Registered " + definitions.size() + " custom aspects from " + directory);
        } catch (IOException | RuntimeException | LinkageError exception) {
            String message = "Could not load custom aspects from " + directory + ": " + exception.getMessage();
            ThaumicDabblery.LOG.error(message, exception);
            MineTweakerAPI.logError(message, exception);
            throw new IllegalStateException(message, exception);
        }
    }

    @SuppressWarnings("rawtypes")
    private static List<Definition> compileAndCollect(List<Path> paths) throws IOException {
        // ZenModule shares these maps process-wide, even for different registries. Do not disturb ContentTweaker
        // or leak __ZenMain__ into the later MineTweaker pass (including older CraftTweaker releases).
        Map<String, byte[]> previousClasses = new HashMap<>(ZenModule.classes);
        Map<String, Class> previousLoaded = new HashMap<>(ZenModule.loadedClasses);
        try {
            ZenModule.classes.clear();
            ZenModule.loadedClasses.clear();
            Errors errors = new Errors();
            GenericCompileEnvironment compileEnvironment = new GenericCompileEnvironment();
            GenericRegistry registry = new GenericRegistry(compileEnvironment, errors);
            registry.registerNativeClass(CustomAspectsZen.class);
            Map<String, byte[]> classes = new HashMap<>();
            IEnvironmentGlobal environment = registry.makeGlobalEnvironment(classes);
            List<ZenParsedFile> parsed = new ArrayList<>();
            int index = 0;
            for (Path path : paths) {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    String filename = path.toString();
                    ZenTokener tokener = new ZenTokener(reader, compileEnvironment, filename, false);
                    parsed.add(new ZenParsedFile(filename, "td_aspect_definition_" + index++, tokener, environment));
                } catch (RuntimeException exception) {
                    throw new IllegalArgumentException("In " + path + ": " + exception.getMessage(), exception);
                }
            }
            ZenModule.compileScripts("Thaumic Dabblery custom aspects", parsed, environment, false);
            if (errors.failed) {
                throw new IllegalArgumentException("Custom aspect script compilation failed; see minetweaker.log");
            }
            collecting = new ArrayList<>();
            new ZenModule(classes, CustomAspectsZen.class.getClassLoader()).getMain()
                .run();
            return new ArrayList<>(collecting);
        } finally {
            collecting = null;
            ZenModule.classes.clear();
            ZenModule.classes.putAll(previousClasses);
            ZenModule.loadedClasses.clear();
            ZenModule.loadedClasses.putAll(previousLoaded);
        }
    }

    private static final class Errors implements IZenErrorLogger {

        private boolean failed;

        @Override
        public void error(ZenPosition position, String message) {
            error(position + ": " + message);
        }

        @Override
        public void warning(ZenPosition position, String message) {
            warning(position + ": " + message);
        }

        @Override
        public void info(ZenPosition position, String message) {
            info(position + ": " + message);
        }

        @Override
        public void error(String message) {
            failed = true;
            MineTweakerAPI.logError(message);
            ThaumicDabblery.LOG.error(message);
        }

        @Override
        public void error(String message, Throwable exception) {
            failed = true;
            MineTweakerAPI.logError(message, exception);
            ThaumicDabblery.LOG.error(message, exception);
        }

        @Override
        public void warning(String message) {
            MineTweakerAPI.logWarning(message);
        }

        @Override
        public void info(String message) {
            MineTweakerAPI.logInfo(message);
        }
    }
}
