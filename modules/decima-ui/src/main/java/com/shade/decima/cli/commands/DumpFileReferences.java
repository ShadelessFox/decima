package com.shade.decima.cli.commands;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.rtti.RTTICoreFileReader.LoggingErrorHandlingStrategy;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.*;

@Command(name = "file-references", description = "Dumps all file references", sortOptions = false)
public class DumpFileReferences implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(DumpFileReferences.class);

    @Option(names = {"-p", "--project"}, required = true, description = "The working project")
    private Project project;

    @Option(names = {"-o", "--output"}, required = true, description = "The output file (.csv)")
    private Path output;

    @Override
    public void run() {
        final var manager = project.getPackfileManager();

        final var index = new AtomicInteger();
        final var total = manager.getArchives().stream()
            .mapToInt(packfile -> packfile.getFileEntries().size())
            .sum();

        final List<String> names = manager.getArchives().parallelStream()
            .flatMap(packfile -> packfile.getFileEntries().parallelStream()
                .flatMap(file -> {
                    try {
                        final List<String> result = new ArrayList<>();

                        project.getCoreFileReader()
                            .read(packfile.getFile(file.hash()), LoggingErrorHandlingStrategy.getInstance())
                            .visitAllObjects(RTTIReference.External.class, ref -> {
                                if (ref.path().isEmpty()) {
                                    return;
                                }

                                result.add("%#018x,%s,%s".formatted(
                                    file.hash(),
                                    Packfile.getNormalizedPath(ref.path()),
                                    RTTIUtils.uuidToString(ref.uuid())
                                ));
                            });

                        return result.stream();
                    } catch (Exception e) {
                        return Stream.empty();
                    } finally {
                        final int current = index.incrementAndGet();
                        if (current % 5000 == 0) {
                            log.info("{}/{} files processed ({}%)", current, total, "%.02f".formatted((float) current / total * 100));
                        }
                    }
                })
            )
            .distinct()
            .sorted()
            .toList();

        try {
            Files.write(output, names, WRITE, CREATE, TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Error writing strings to the output file", e);
        }
    }
}
