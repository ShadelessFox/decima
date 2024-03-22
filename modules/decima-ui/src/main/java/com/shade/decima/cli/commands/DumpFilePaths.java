package com.shade.decima.cli.commands;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.*;

@Command(name = "paths", description = "Dumps all valid file paths from packfiles of the selected project", sortOptions = false)
public class DumpFilePaths implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(DumpFilePaths.class);

    private static final String[] VALID_MOUNT_PREFIXES = {"appdir:", "cache:", "source:", "work:"};
    private static final String[] VALID_FILE_EXTENSIONS = {".core", ".stream", ".core.stream", ".streaming.core", ".coretext", ".coredebug", ".dep"};

    @Option(names = {"-p", "--project"}, required = true, description = "The working project")
    private Project project;

    @Option(names = {"-o", "--output"}, required = true, description = "The output file (.txt)")
    private Path output;

    @Override
    public void run() {
        final var manager = project.getPackfileManager();
        final var registry = project.getTypeRegistry();

        final var entries = manager.getArchives().stream()
            .map(Packfile::getFileEntries)
            .flatMap(Collection::stream)
            .map(Packfile.FileEntry::hash)
            .collect(Collectors.toSet());

        final var languages = Arrays.stream(((RTTITypeEnum) registry.find("ELanguage")).values())
            .map(RTTITypeEnum.Constant::name)
            .map(String::toLowerCase)
            .distinct()
            .toArray(String[]::new);

        final int total = manager.getArchives().stream()
            .mapToInt(packfile -> packfile.getFileEntries().size())
            .sum();
        final AtomicInteger index = new AtomicInteger();

        log.info("Files found: {} (unique files: {})", total, entries.size());

        final Set<String> paths = manager.getArchives().parallelStream()
            .flatMap(packfile -> packfile.getFileEntries().parallelStream()
                .flatMap(file -> {
                    try {
                        final Set<String> result = new HashSet<>();

                        project.getCoreFileReader()
                            .read(packfile.getFile(file.hash()), true)
                            .visitAllObjects(String.class, string -> {
                                if (!string.isEmpty()) {
                                    result.add(string);
                                }
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
                .map(DumpFilePaths::getStrippedPath)
                .flatMap(path -> Stream.concat(
                    Arrays.stream(VALID_FILE_EXTENSIONS).map(ext -> path + ext),
                    Stream.concat(
                        Arrays.stream(languages).map(lang -> path + "." + lang + ".stream"),
                        Arrays.stream(languages).map(lang -> path + ".wem." + lang + ".core.stream")
                    )
                ))
                .filter(path -> entries.contains(Packfile.getPathHash(path)))
            )
            .collect(TreeSet::new, Set::add, Set::addAll);

        try {
            Files.write(output, paths, WRITE, CREATE, TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Error writing strings to the output file", e);
        }
    }

    @NotNull
    private static String getStrippedPath(@NotNull String path) {
        String stripped = path;

        for (String prefix : VALID_MOUNT_PREFIXES) {
            if (stripped.toLowerCase().startsWith(prefix)) {
                stripped = stripped.substring(prefix.length());
                break;
            }
        }

        return stripped
            .replace(".core.stream", "")
            .replace(".core", "");
    }
}
