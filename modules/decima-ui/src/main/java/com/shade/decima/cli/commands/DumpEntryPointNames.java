package com.shade.decima.cli.commands;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.rtti.RTTICoreFileReader.LoggingErrorHandlingStrategy;
import com.shade.util.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.*;

@Command(name = "entry-points", description = "Dumps all script entry point names and their checksums", sortOptions = false)
public class DumpEntryPointNames implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(DumpEntryPointNames.class);

    @Option(names = {"-p", "--project"}, required = true, description = "The working project")
    private Project project;

    @Option(names = {"-o", "--output"}, required = true, description = "The output file (.csv)")
    private Path output;

    @Override
    public void run() {
        final var manager = project.getPackfileManager();
        final var registry = project.getTypeRegistry();

        final var index = new AtomicInteger();
        final var total = manager.getArchives().stream()
            .mapToInt(packfile -> packfile.getFileEntries().size())
            .sum();

        final List<String> names = manager.getArchives().parallelStream()
            .flatMap(packfile -> packfile.getFileEntries().parallelStream()
                .flatMap(file -> {
                    try {
                        final Set<String> result = new HashSet<>();

                        project.getCoreFileReader()
                            .read(packfile.getFile(file.hash()), LoggingErrorHandlingStrategy.getInstance())
                            .visitAllObjects("ProgramResourceEntryPoint", object -> {
                                result.add(object.str("EntryPoint"));
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
            .sorted()
            .map(name -> "0x%s,%s".formatted(Hashing.decimaCrc32().hashString(name), name))
            .toList();

        try {
            Files.write(output, names, WRITE, CREATE, TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Error writing strings to the output file", e);
        }
    }
}
