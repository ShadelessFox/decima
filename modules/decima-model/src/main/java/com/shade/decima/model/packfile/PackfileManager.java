package com.shade.decima.model.packfile;

import com.shade.decima.model.archive.ArchiveFile;
import com.shade.decima.model.archive.ArchiveManager;
import com.shade.decima.model.packfile.edit.Change;
import com.shade.decima.model.util.Compressor;
import com.shade.decima.model.util.FilePath;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.shade.decima.model.packfile.Packfile.*;

public class PackfileManager implements ArchiveManager {
    private static final Logger log = LoggerFactory.getLogger(PackfileManager.class);

    private final NavigableSet<Packfile> packfiles = new TreeSet<>();
    private final Compressor compressor;

    public PackfileManager(@NotNull Compressor compressor) {
        this.compressor = compressor;
    }

    public void mountPackfile(@NotNull PackfileInfo info) throws IOException {
        if (Files.notExists(info.path())) {
            return;
        }

        final Packfile packfile = new Packfile(this, compressor, info);

        synchronized (this) {
            if (!packfiles.add(packfile)) {
                log.error("Packfile '{}' already mounted", info.path());
                return;
            }
        }

        log.info("Mounted '{}'", info.path());
    }

    @NotNull
    public Packfile openPackfile(@NotNull Path path) throws IOException {
        return new Packfile(this, compressor, new PackfileInfo(path, IOUtils.getBasename(path), null));
    }

    @Nullable
    @Override
    public ArchiveFile findFile(@NotNull String identifier) {
        final Packfile archive = findFirst(identifier);
        if (archive == null) {
            return null;
        }
        return archive.getFile(identifier);
    }

    @Nullable
    @Override
    public ArchiveFile findFile(long identifier) {
        final Packfile archive = findFirst(identifier);
        if (archive == null) {
            return null;
        }
        return archive.getFile(identifier);
    }

    @NotNull
    @Override
    public Collection<Packfile> getArchives() {
        return packfiles;
    }

    @Nullable
    public Packfile findFirst(@NotNull String path) {
        return findFirst(getPathHash(getNormalizedPath(path)));
    }

    @Nullable
    public Packfile findFirst(long hash) {
        // Process in descending order, so patch packfile will be first (as it has the highest priority), if present
        return packfiles.descendingSet().stream()
            .filter(x -> x.getFileEntry(hash) != null)
            .findAny().orElse(null);
    }

    public boolean hasChanges() {
        for (Packfile packfile : packfiles) {
            if (packfile.hasChanges()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasChangesInPath(@NotNull FilePath path) {
        for (Packfile packfile : packfiles) {
            if (packfile.hasChangesInPath(path)) {
                return true;
            }
        }

        return false;
    }

    public boolean canMergeChanges() {
        final Map<FilePath, Packfile> changes = new HashMap<>();

        for (Packfile packfile : packfiles) {
            for (Map.Entry<FilePath, Change> change : packfile.getChanges().entrySet()) {
                final Packfile previousPackfile = changes.get(change.getKey());

                if (previousPackfile != null && previousPackfile != packfile) {
                    return false;
                }

                changes.put(change.getKey(), packfile);
            }
        }

        return true;
    }

    @Override
    public void close() throws IOException {
        for (Packfile packfile : packfiles) {
            packfile.close();
        }

        packfiles.clear();
    }
}
