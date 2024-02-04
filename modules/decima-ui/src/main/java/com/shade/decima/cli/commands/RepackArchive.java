package com.shade.decima.cli.commands;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.packfile.PackfileWriter;
import com.shade.decima.model.packfile.edit.Change;
import com.shade.decima.model.packfile.edit.FileChange;
import com.shade.decima.model.packfile.prefetch.PrefetchUpdater;
import com.shade.decima.model.packfile.resource.PackfileResource;
import com.shade.decima.model.util.Oodle;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.*;
import static picocli.CommandLine.Help.Visibility.ALWAYS;

@Command(name = "repack", description = "Add or overwrite files in the selected archive", sortOptions = false)
public class RepackArchive implements Callable<Void> {
    private static final Logger log = LoggerFactory.getLogger(RepackArchive.class);

    @Option(names = {"-p", "--project"}, required = true, description = "The working project")
    private Project project;

    @Parameters(index = "0", description = "The archive to repack.")
    private Path path;

    @Parameters(index = "1", description = "The directory containing files to be added to the archive.")
    private Path input;

    @Option(names = {"-b", "--backup"}, description = "Create a backup of the destination archive.", showDefaultValue = ALWAYS)
    private boolean backup;

    @Option(names = {"-e", "--encrypt"}, description = "Encrypt the archive (supported by DS only).", showDefaultValue = ALWAYS)
    private boolean encrypt;

    @Option(names = {"-t", "--truncate"}, description = "Truncate the archive.", showDefaultValue = ALWAYS)
    private boolean truncate;

    @Option(names = {"-l", "--level"}, description = "Compression level. Valid values (from faster repack/bigger file to slower repack/smaller file): ${COMPLETION-CANDIDATES}", showDefaultValue = ALWAYS)
    private Oodle.CompressionLevel compression = Oodle.CompressionLevel.FAST;

    @Option(names = {"--no-rebuild-prefetch"}, description = "Rebuild prefetch data", negatable = true, showDefaultValue = ALWAYS)
    private boolean rebuildPrefetch = true;

    @Option(names = {"--no-changed-files-only"}, description = "Update only changed files in the prefetch. Requires '--rebuild-prefetch'", negatable = true, showDefaultValue = ALWAYS)
    private boolean updateChangedFilesOnly = true;

    @Override
    public Void call() throws Exception {
        final Oodle oodle = project.getCompressor();
        final Packfile source;

        if (truncate) {
            source = null;
        } else if (Files.exists(path)) {
            source = new Packfile(path, oodle);
        } else {
            log.warn("The specified archive file does not exist: " + path);
            source = null;
        }

        try (PackfileWriter writer = new PackfileWriter()) {
            log.info("Collecting input files from {}", input.toAbsolutePath());

            final Map<Long, Change> changes = collectChanges(input);

            if (changes.isEmpty()) {
                log.info("No files to add/overwrite, aborting the process");
                return null;
            }

            if (rebuildPrefetch) {
                log.info("Rebuilding prefetch data");

                final PrefetchUpdater.ChangeInfo prefetch = PrefetchUpdater.rebuildPrefetch(
                    new VoidProgressMonitor(),
                    project,
                    updateChangedFilesOnly
                        ? PrefetchUpdater.FileSupplier.ofChanged(changes.values())
                        : PrefetchUpdater.FileSupplier.ofAll(changes.values(), project.getPackfileManager())
                );

                if (prefetch != null) {
                    final Change change = prefetch.change();
                    if (changes.put(change.hash(), change) != null) {
                        log.warn("Prefetch file is already in the list of changes and was overridden");
                    }
                }
            }

            for (Change resource : changes.values()) {
                writer.add(resource.toResource());
            }

            if (source != null) {
                for (PackfileBase.FileEntry entry : source.getFileEntries()) {
                    if (changes.containsKey(entry.hash())) {
                        continue;
                    }

                    writer.add(new PackfileResource(source, entry));
                }
            }

            if (backup && Files.exists(path)) {
                try {
                    final Path backup = IOUtils.makeBackupPath(this.path);
                    log.info("Creating backup to {}", backup);
                    Files.move(path, backup);
                } catch (IOException e) {
                    log.error("Unable to create backup", e);
                }
            }

            final Path result = Path.of(path + ".tmp");

            try (FileChannel channel = FileChannel.open(result, WRITE, CREATE, TRUNCATE_EXISTING)) {
                log.info("Writing data to {}", result.toAbsolutePath());
                // TODO: Use console progress monitor here!!!
                writer.write(new VoidProgressMonitor(), channel, oodle, new PackfileWriter.Options(compression, encrypt));
            }

            Files.move(result, path, REPLACE_EXISTING);

            log.info("Done");
        }

        return null;
    }

    @NotNull
    private static Map<Long, Change> collectChanges(@NotNull Path root) throws IOException {
        final Map<Long, Change> changes = new HashMap<>();

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                final String path = PackfileBase.getNormalizedPath(root.relativize(file).toString());
                final long hash = PackfileBase.getPathHash(path);

                log.info("Found {}", path);
                changes.put(hash, new FileChange(file, hash));

                return FileVisitResult.CONTINUE;
            }
        });

        return changes;
    }
}
