package com.shade.decima.cli.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.archive.ArchiveFile;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.RTTICoreFileReader.ThrowingErrorHandlingStrategy;
import com.shade.decima.model.rtti.RTTIEnum;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.java.HwLocalizedText;
import com.shade.decima.model.rtti.types.java.HwLocalizedText.DisplayMode;
import com.shade.platform.model.util.AlphanumericComparator;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(name = "localization", description = "Localization-related commands", subcommands = {
    Localization.Export.class,
    Localization.Import.class
})
public class Localization {
    private static final Logger log = LoggerFactory.getLogger(Localization.class);
    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(DisplayMode.class, new DisplayModeAdapter())
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    @Command(name = "export", description = "Exports localization data")
    public static class Export implements Callable<Void> {
        private static final String[] VALID_FILE_NAMES = {"simpletext.core", "sentences.core"};

        @Option(names = {"-p", "--project"}, required = true, description = "The working project")
        private Project project;

        @Option(names = {"-o", "--output"}, required = true, description = "The output file (.json)")
        private Path output;

        @Option(names = {"-s", "--source"}, required = true, description = "The source language")
        private String source;

        @Option(names = {"-t", "--target"}, required = true, description = "The target language")
        private String target;

        @Override
        public Void call() throws Exception {
            final PackfileManager packfileManager = project.getPackfileManager();
            final RTTITypeRegistry typeRegistry = project.getTypeRegistry();

            final RTTIEnum languages = typeRegistry.find("ELanguage");
            final RTTIEnum.Constant sourceLanguage = languages.valueOf(source);
            final RTTIEnum.Constant targetLanguage = languages.valueOf(target);

            final String[] paths = getPaths();
            final FileSchema schema = toSchema(project, paths, packfileManager, sourceLanguage, targetLanguage);

            try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                gson.toJson(schema, writer);
            }

            return null;
        }

        @NotNull
        private FileSchema toSchema(
            @NotNull Project project,
            @NotNull String[] paths,
            @NotNull PackfileManager packfileManager,
            @NotNull RTTIEnum.Constant sourceLanguage,
            @NotNull RTTIEnum.Constant targetLanguage
        ) {
            final Map<String, Map<String, TextSchema>> files = new LinkedHashMap<>();

            for (int i = 0; i < paths.length; i++) {
                final String path = paths[i];
                log.info("[{}/{}] Exporting {}", i + 1, paths.length, path);

                final ArchiveFile file = packfileManager.findFile(path);
                if (file == null) {
                    log.warn("Can't find localization file '{}'", path);
                    continue;
                }

                final RTTICoreFile core;
                try {
                    core = project.getCoreFileReader().read(
                        file,
                        ThrowingErrorHandlingStrategy.getInstance()
                    );
                } catch (Exception e) {
                    log.warn("Unable to read '{}': {}", path, e.getMessage());
                    continue;
                }

                var texts = new LinkedHashMap<String, TextSchema>();
                var voices = new HashMap<String, VoiceSchema>();

                for (RTTIObject object : core.objects()) {
                    if (!object.type().isInstanceOf("SentenceResource")) {
                        continue;
                    }
                    try {
                        var text = object.ref("Text").get(project, core);
                        var voice = object.ref("Voice").get(project, core);
                        if (text == null || voice == null) {
                            continue;
                        }
                        var name = voice.ref("NameResource").get(project, core).obj("Data").<HwLocalizedText>cast();
                        var uuid = RTTIUtils.uuidToString(text.uuid());
                        voices.put(uuid, new VoiceSchema(
                            voice.str("Gender"),
                            name.getTranslation(sourceLanguage.value() - 1)
                        ));
                    } catch (IOException e) {
                        log.warn("Unable to read sentence '{}': {}", object.uuid(), e.getMessage());
                    }
                }

                for (RTTIObject object : core.objects()) {
                    if (!object.type().isInstanceOf("LocalizedTextResource")) {
                        continue;
                    }
                    var text = object.obj("Data").<HwLocalizedText>cast();
                    var uuid = RTTIUtils.uuidToString(object.uuid());
                    var schema = new TextSchema(
                        text.getTranslation(sourceLanguage.value() - 1),
                        text.getTranslation(targetLanguage.value() - 1),
                        voices.get(uuid),
                        text.getDisplayMode(targetLanguage.value() - 1)
                    );

                    if (isEmpty(schema.source) && isEmpty(schema.target)) {
                        log.warn("Skipping empty text '{}'", uuid);
                        continue;
                    }

                    texts.put(uuid, schema);
                }

                if (!texts.isEmpty()) {
                    files.put(path, texts);
                }
            }

            return new FileSchema(source, target, files);
        }

        @NotNull
        private String[] getPaths() throws IOException {
            final String[] paths;
            try (Stream<String> stream = project.listAllFiles()) {
                paths = stream
                    .filter(path -> IOUtils.contains(VALID_FILE_NAMES, IOUtils.getFilename(path)))
                    .sorted(AlphanumericComparator.getInstance())
                    .toArray(String[]::new);
            }
            return paths;
        }

        private static boolean isEmpty(@NotNull String text) {
            final String trimmed = text.trim();
            return trimmed.isEmpty() || trimmed.equals("(none)") || trimmed.equals("(did not translate)") || trimmed.equals("<ignoresub>");
        }
    }

    @Command(name = "import", description = "Imports localization data")
    public static class Import implements Callable<Void> {
        @Option(names = {"-p", "--project"}, required = true, description = "The working project")
        private Project project;

        @Option(names = {"-i", "--input"}, required = true, description = "The input file (.json)")
        private Path input;

        @Option(names = {"-o", "--output"}, required = true, description = "The output directory for patched .core files")
        private Path output;

        @Override
        public Void call() throws Exception {
            final PackfileManager packfileManager = project.getPackfileManager();
            final RTTITypeRegistry typeRegistry = project.getTypeRegistry();

            log.info("Importing localization data from {}", input);

            try (Reader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
                final FileSchema schema = gson.fromJson(reader, FileSchema.class);

                final RTTIEnum languages = typeRegistry.find("ELanguage");
                final RTTIEnum.Constant sourceLanguage = languages.valueOf(schema.source);
                final RTTIEnum.Constant targetLanguage = languages.valueOf(schema.target);

                log.info("Source language: {}", sourceLanguage);
                log.info("Target language: {}", targetLanguage);

                for (Map.Entry<String, Map<String, TextSchema>> entry : schema.files.entrySet()) {
                    final String path = entry.getKey();
                    log.info("Reading {}", path);

                    final ArchiveFile file = packfileManager.findFile(path);
                    if (file == null) {
                        log.warn("Can't find localization file '{}'", path);
                        continue;
                    }

                    final RTTICoreFile core;
                    try {
                        core = project.getCoreFileReader().read(file, ThrowingErrorHandlingStrategy.getInstance());
                    } catch (Exception e) {
                        log.warn("Unable to read '{}': {}", path, e.getMessage());
                        continue;
                    }

                    boolean dirty = false;

                    final Map<String, RTTIObject> objects = new HashMap<>();
                    for (RTTIObject object : core.objects()) {
                        objects.put(RTTIUtils.uuidToString(object.uuid()), object);
                    }

                    for (Map.Entry<String, TextSchema> e : entry.getValue().entrySet()) {
                        final RTTIObject object = objects.get(e.getKey());
                        if (object == null) {
                            log.warn("Can't find object '{}'", e.getKey());
                            continue;
                        }

                        final HwLocalizedText text = object.obj("Data").cast();
                        final TextSchema translation = e.getValue();

                        if (!text.getTranslation(targetLanguage.value() - 1).equals(translation.target)) {
                            text.setTranslation(targetLanguage.value() - 1, translation.target);
                            dirty = true;
                        }

                        if (text.getDisplayMode(targetLanguage.value() - 1) != translation.show) {
                            text.setDisplayMode(targetLanguage.value() - 1, translation.show);
                            dirty = true;
                        }
                    }

                    if (dirty) {
                        final Path target = output.resolve(path);
                        log.info("Found changes, writing to {}", target);

                        Files.createDirectories(target.getParent());
                        Files.write(target, project.getCoreFileReader().write(core));
                    }
                }
            }

            return null;
        }
    }

    private static class DisplayModeAdapter extends TypeAdapter<DisplayMode> {
        @Override
        public void write(JsonWriter out, DisplayMode value) throws IOException {
            switch (value) {
                case SHOW_IF_SUBTITLES_ENABLED -> out.value("auto");
                case SHOW_ALWAYS -> out.value("always");
                case SHOW_NEVER -> out.value("never");
            }
        }

        @Override
        public DisplayMode read(JsonReader in) throws IOException {
            return switch (in.nextString()) {
                case "auto" -> DisplayMode.SHOW_IF_SUBTITLES_ENABLED;
                case "always" -> DisplayMode.SHOW_ALWAYS;
                case "never" -> DisplayMode.SHOW_NEVER;
                default -> throw new IllegalStateException("Unexpected value: " + in.nextString());
            };
        }
    }

    private record FileSchema(@NotNull String source, @NotNull String target, @NotNull Map<String, Map<String, TextSchema>> files) {
    }

    private record TextSchema(@NotNull String source, @NotNull String target, @Nullable VoiceSchema voice, @NotNull DisplayMode show) {
    }

    private record VoiceSchema(@NotNull String gender, @NotNull String name) {
    }
}
