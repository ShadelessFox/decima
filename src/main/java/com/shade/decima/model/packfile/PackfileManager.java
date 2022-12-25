package com.shade.decima.model.packfile;

import com.shade.decima.model.packfile.edit.Change;
import com.shade.decima.model.util.Compressor;
import com.shade.decima.ui.navigator.impl.FilePath;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static com.shade.decima.model.packfile.PackfileBase.*;

public class PackfileManager implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(PackfileManager.class);

    private static final String PACKFILE_EXTENSION = ".bin";

    private final Compressor compressor;
    private final SortedSet<Packfile> packfiles;
    private final Map<String, PackfileInfo> metadata;

    public PackfileManager(@NotNull Compressor compressor, @Nullable Map<String, PackfileInfo> info) {
        this.compressor = compressor;
        this.packfiles = new TreeSet<>();
        this.metadata = info;
    }

    public void mount(@NotNull Path packfile) throws IOException {
        if (Files.notExists(packfile)) {
            return;
        }

        String name = packfile.getFileName().toString();

        if (name.indexOf('.') >= 0) {
            name = name.substring(0, name.indexOf('.'));
        }

        final PackfileInfo info = metadata != null
            ? metadata.get(name)
            : null;

        packfiles.add(new Packfile(packfile, compressor, info));
        log.info("Mounted '{}'", packfile);
    }

    public void mountDefaults(@NotNull Path root) throws IOException {
        try (Stream<Path> stream = listPackfiles(root).parallel()) {
            stream.filter(PackfileManager::isValidPackfile).forEach(path -> {
                try {
                    mount(path);
                } catch (IOException e) {
                    log.error("Unable to mount packfile '" + path + "'", e);
                }
            });
        }
    }

    @Nullable
    public Packfile findAny(@NotNull String path) {
        return findAny(getPathHash(getNormalizedPath(path)));
    }

    @Nullable
    public Packfile findAny(long hash) {
        return packfiles.stream()
            .filter(x -> x.getFileEntry(hash) != null)
            .findAny().orElse(null);
    }

    @NotNull
    public List<Packfile> findAll(@NotNull String path) {
        return findAll(getPathHash(getNormalizedPath(path)));
    }

    @NotNull
    public List<Packfile> findAll(long hash) {
        return packfiles.stream()
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

    @NotNull
    private Stream<Path> listPackfiles(@NotNull Path root) throws IOException {
        if (metadata != null) {
            return metadata
                .keySet().stream()
                .map(name -> root.resolve(name + PACKFILE_EXTENSION));
        } else {
            return Files.list(root);
        }
    }

    private static boolean isValidPackfile(@NotNull Path path) {
        return path.getFileName().toString().endsWith(PACKFILE_EXTENSION);
    }
}
