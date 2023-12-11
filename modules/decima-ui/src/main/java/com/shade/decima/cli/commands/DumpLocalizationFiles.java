package com.shade.decima.cli.commands;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.java.HwLocalizedText;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(name = "dump-localization", description = "Dumps all localization resources into CSV files", sortOptions = false)
public class DumpLocalizationFiles implements Callable<Void> {
    private static final Logger log = LoggerFactory.getLogger(DumpLocalizationFiles.class);

    private static final String[] VALID_FILE_NAMES = {"simpletext.core"};

    @Option(names = {"-p", "--project"}, required = true, description = "The project to dump from")
    private Project project;

    @Option(names = {"-o", "--output"}, required = true, description = "The output directory")
    private Path output;

    @Override
    public Void call() throws IOException {
        final PackfileManager manager = project.getPackfileManager();
        final RTTITypeRegistry registry = project.getTypeRegistry();
        final String[] paths;

        try (Stream<String> stream = project.listAllFiles()) {
            paths = stream
                .filter(path -> IOUtils.indexOf(VALID_FILE_NAMES, IOUtils.getFilename(path)) >= 0)
                .toArray(String[]::new);
        }

        log.info("Files found: {}", paths.length);

        for (int i = 0; i < paths.length; i++) {
            final String path = paths[i];
            final Packfile packfile = manager.findFirst(path);

            if (packfile == null) {
                log.warn("Can't find file {}", path);
                continue;
            }

            final CoreBinary binary;

            try {
                binary = CoreBinary.from(packfile.extract(path), registry, false);
            } catch (Exception e) {
                log.warn("Unable to read '{}': {}", path, e.getMessage());
                continue;
            }

            final Path target = output.resolve(path + ".csv");

            try {
                dump(binary, target);
            } catch (IOException e) {
                log.warn("Unable to dump '{}': {}", path, e.getMessage());
            }

            log.info("[{}/{}] Dumped {} to {}", i + 1, paths.length, path, target);
        }

        return null;
    }

    private static void dump(@NotNull CoreBinary binary, @NotNull Path output) throws IOException {
        Files.createDirectories(output.getParent());

        final List<RTTIObject> objects = new ArrayList<>();
        binary.visitAllObjects("LocalizedTextResource", objects::add);

        try (OutputStream stream = Files.newOutputStream(output);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8))
        ) {
            // Write BOM
            stream.write(0xEF);
            stream.write(0xBB);
            stream.write(0xBF);

            for (int i = 0; i < objects.size(); i++) {
                final RTTIObject object = objects.get(i);
                final HwLocalizedText text = object.obj("Data").cast();

                if (i == 0) {
                    writer.write("Key");

                    for (int j = 0; j < text.getLocalizationCount(); j++) {
                        writer.write(',');
                        writer.write(text.getLocalizationLanguage(j));
                    }
                }

                writer.newLine();
                writer.write(RTTIUtils.uuidToString(object.obj("ObjectUUID")));

                for (int j = 0; j < text.getLocalizationCount(); j++) {
                    final String value = text.getLocalizationText(j);
                    writer.write(",\"" + value.replaceAll("[\r\n\"]", "\\$1") + "\"");
                }
            }
        }
    }

}
