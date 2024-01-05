package com.shade.decima.model.packfile;

import com.shade.decima.model.packfile.edit.Change;
import com.shade.decima.model.util.FilePath;
import com.shade.decima.model.util.Oodle;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static com.shade.decima.model.packfile.PackfileBase.*;

public class PackfileManager implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(PackfileManager.class);

    private final NavigableSet<Packfile> packfiles;

    public PackfileManager() {
        this.packfiles = new TreeSet<>();
    }

    public void mount(@NotNull PackfileInfo info, @NotNull Oodle oodle) throws IOException {
        if (Files.notExists(info.path())) {
            return;
        }

        final Packfile packfile = new Packfile(info, oodle);

        synchronized (this) {
            if (!packfiles.add(packfile)) {
                log.error("Packfile '{}' already mounted", info.path());
                return;
            }
        }

        log.info("Mounted '{}'", info.path());
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

    @NotNull
    public List<Packfile> findAll(@NotNull String path) {
        return findAll(getPathHash(getNormalizedPath(path)));
    }

    @NotNull
    public List<Packfile> findAll(long hash) {
        // Process in descending order, so patch packfile will be first (as it has the highest priority), if present
        return packfiles.descendingSet().stream()
            .filter(x -> x.getFileEntry(hash) != null)
            .toList();
    }

    public boolean hasChanges() {
        for (Packfile packfile : packfiles) {
            if (packfile.hasChanges()) {
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

    @NotNull
    public Collection<Packfile> getPackfiles() {
        return packfiles;
    }

    @Override
    public void close() throws IOException {
        for (Packfile packfile : packfiles) {
            packfile.close();
        }

        packfiles.clear();
    }
}
