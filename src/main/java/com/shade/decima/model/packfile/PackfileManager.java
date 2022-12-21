package com.shade.decima.model.packfile;

import com.shade.decima.model.packfile.edit.Change;
import com.shade.decima.model.util.Compressor;
import com.shade.decima.ui.navigator.impl.FilePath;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.EventListenerList;
import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static com.shade.decima.model.packfile.PackfileBase.*;

public class PackfileManager implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(PackfileManager.class);

    private static final String PACKFILE_EXTENSION = ".bin";

    private final Compressor compressor;
    private final SortedSet<Packfile> packfiles;
    private final Map<String, PackfileInfo> metadata;
    private final Map<Packfile, Map<FilePath, List<Change>>> changes;
    private final EventListenerList listeners = new EventListenerList();

    public PackfileManager(@NotNull Compressor compressor, @Nullable Map<String, PackfileInfo> info) {
        this.compressor = compressor;
        this.packfiles = new TreeSet<>();
        this.metadata = info;
        this.changes = new HashMap<>();
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

        packfiles.add(new Packfile(
            FileChannel.open(packfile, StandardOpenOption.READ),
            compressor,
            info,
            packfile
        ));

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

    @NotNull
    public Map<Packfile, Map<FilePath, List<Change>>> getChanges() {
        return changes;
    }

    @NotNull
    public Map<Packfile, Map<FilePath, Change>> getMergedChanges() {
        final Map<Packfile, Map<FilePath, Change>> merged = new HashMap<>();

        for (var changesPerPackfile : changes.entrySet()) {
            for (var changesPerPath : changesPerPackfile.getValue().entrySet()) {
                final List<Change> changes = changesPerPath.getValue();

                if (!changes.isEmpty()) {
                    Change result = changes.get(0);

                    for (int i = 1; i < changes.size(); i++) {
                        result = result.merge(changes.get(i));
                    }

                    merged
                        .computeIfAbsent(changesPerPackfile.getKey(), x -> new HashMap<>(1))
                        .put(changesPerPath.getKey(), result);
                }
            }
        }

        return merged;
    }

    public void addChange(@NotNull Packfile packfile, @NotNull FilePath path, @NotNull Change change) {
        changes
            .computeIfAbsent(packfile, x -> new HashMap<>(1))
            .computeIfAbsent(path, x -> new ArrayList<>(1))
            .add(change);

        for (PackfileChangeListener listener : listeners.getListeners(PackfileChangeListener.class)) {
            listener.changeAdded(packfile, path, change);
        }
    }

    public void removeChanges(@NotNull Packfile packfile, @NotNull FilePath path) {
        final Map<FilePath, List<Change>> packfileChanges = changes.get(packfile);

        if (packfileChanges != null) {
            final List<Change> changes = packfileChanges.remove(path);

            if (changes != null) {
                for (Change change : changes) {
                    for (PackfileChangeListener listener : listeners.getListeners(PackfileChangeListener.class)) {
                        listener.changeRemoved(packfile, path, change);
                    }
                }
            }
        }
    }

    public void clearChanges() {
        for (var changesForPackfile : changes.entrySet()) {
            for (var changesForPath : changesForPackfile.getValue().entrySet()) {
                for (Change change : changesForPath.getValue()) {
                    for (PackfileChangeListener listener : listeners.getListeners(PackfileChangeListener.class)) {
                        listener.changeRemoved(changesForPackfile.getKey(), changesForPath.getKey(), change);
                    }
                }
            }
        }

        changes.clear();
    }

    public boolean hasChanges() {
        for (var changesPerPackfile : changes.values()) {
            for (var changesPerPath : changesPerPackfile.values()) {
                if (!changesPerPath.isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean hasChanges(@NotNull Packfile packfile, @NotNull FilePath path) {
        final var changesForPackfile = changes.get(packfile);
        final var changesForPath = changesForPackfile != null ? changesForPackfile.get(path) : null;

        return changesForPath != null && !changesForPath.isEmpty();
    }

    public boolean hasChangesInPath(@NotNull Packfile packfile, @NotNull FilePath path) {
        final var changesForPackfile = changes.get(packfile);

        if (changesForPackfile != null) {
            for (FilePath changed : changesForPackfile.keySet()) {
                if (changed.startsWith(path)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean canMergeChanges() {
        final Map<FilePath, Packfile> paths = new HashMap<>();

        for (var changesForPackfile : changes.entrySet()) {
            for (var changesForPath : changesForPackfile.getValue().entrySet()) {
                final Packfile packfile = paths.get(changesForPath.getKey());

                if (packfile != null && packfile != changesForPackfile.getKey()) {
                    return false;
                }

                paths.put(changesForPath.getKey(), changesForPackfile.getKey());
            }
        }

        return true;
    }

   /*@NotNull
    public Collection<Long> getChangedFiles() {
        return changes.keySet();
    }

    @NotNull
    public Change getMergedChange(@NotNull Packfile packfile, @NotNull FilePath path) {
        final List<Change> changes = this.changes.get(hash);

        if (changes == null || changes.isEmpty()) {
            throw new IllegalStateException("No changes");
        }

        Change merged = changes.get(0);

        for (int i = 1; i < changes.size(); i++) {
            merged = merged.merge(changes.get(i));
        }

        return merged;
    }*/

    public void fireChangeEvent(@NotNull Change change, @NotNull BiConsumer<PackfileChangeListener, Change> consumer) {
        for (PackfileChangeListener listener : listeners.getListeners(PackfileChangeListener.class)) {
            consumer.accept(listener, change);
        }
    }

    public void addChangeListener(@NotNull PackfileChangeListener listener) {
        listeners.add(PackfileChangeListener.class, listener);
    }

    public void removeChangeListener(@NotNull PackfileChangeListener listener) {
        listeners.remove(PackfileChangeListener.class, listener);
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
