package com.shade.decima.model.packfile.prefetch;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.archive.ArchiveFile;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.packfile.edit.Change;
import com.shade.decima.model.packfile.edit.MemoryChange;
import com.shade.decima.model.packfile.resource.PackfileResource;
import com.shade.decima.model.packfile.resource.Resource;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.RTTICoreFileReader.ThrowingErrorHandlingStrategy;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.util.FilePath;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PrefetchUpdater {
    public static final String PREFETCH_PATH = "prefetch/fullgame.prefetch.core";
    private static final Logger log = LoggerFactory.getLogger(PrefetchUpdater.class);

    private PrefetchUpdater() {
        // prevents instantiation
    }

    @Nullable
    public static ChangeInfo rebuildPrefetch(@NotNull ProgressMonitor monitor, @NotNull Project project, @NotNull FileSupplier fileSupplier) throws IOException {
        final PackfileManager packfileManager = project.getPackfileManager();
        final Packfile packfile = packfileManager.findFirst(PREFETCH_PATH);

        if (packfile == null) {
            log.error("Can't find prefetch file");
            return null;
        }

        final ArchiveFile file = packfile.getFile(PREFETCH_PATH);
        final RTTICoreFile core = project.getCoreFileReader().read(file, ThrowingErrorHandlingStrategy.getInstance());

        if (core.objects().isEmpty()) {
            log.error("Prefetch file is empty");
            return null;
        }

        final RTTIObject object = core.objects().get(0);
        final PrefetchList prefetch = PrefetchList.of(object);

        rebuildPrefetch(prefetch, monitor, project, fileSupplier);
        updatePrefetch(prefetch, object);

        final byte[] data = project.getCoreFileReader().write(core);
        final FilePath path = FilePath.of(PREFETCH_PATH, true);
        final Change change = new MemoryChange(data, path.hash());

        return new ChangeInfo(packfile, path, change);
    }

    private static void updatePrefetch(@NotNull PrefetchList prefetch, @NotNull RTTIObject target) {
        final var links = prefetch.links();
        final var count = Arrays.stream(links).mapToInt(x -> x.length).sum() + links.length;
        final var result = new int[count];

        for (int i = 0, j = 0; i < links.length; i++, j++) {
            final int[] src = links[i];
            result[j] = src.length;
            System.arraycopy(src, 0, result, j + 1, src.length);
            j += src.length;
        }

        target.set("Links", result);
        target.set("Sizes", prefetch.sizes());
    }

    private static void rebuildPrefetch(
        @NotNull PrefetchList prefetch,
        @NotNull ProgressMonitor monitor,
        @NotNull Project project,
        @NotNull FileSupplier fileSupplier
    ) {
        final Map<String, Integer> fileIndexLookup = new HashMap<>();
        for (int i = 0; i < prefetch.files().length; i++) {
            fileIndexLookup.put(prefetch.files()[i], i);
        }

        final Map<String, Resource> fileResourceLookup = new HashMap<>();
        for (String path : prefetch.files()) {
            final Resource resource;
            try {
                resource = fileSupplier.get(Packfile.getPathHash(Packfile.getNormalizedPath(path)));
            } catch (IOException e) {
                log.error("Unable to get resource for '{}': {}", path, e.getMessage());
                continue;
            }
            if (resource != null) {
                fileResourceLookup.put(path, resource);
            }
        }

        try (ProgressMonitor.Task task = monitor.begin("Update prefetch", fileResourceLookup.size())) {
            for (Map.Entry<String, Resource> entry : fileResourceLookup.entrySet()) {
                if (task.isCanceled()) {
                    return;
                }

                rebuildFile(prefetch, project, entry.getKey(), entry.getValue(), fileIndexLookup);
                task.worked(1);
            }
        }
    }

    private static void rebuildFile(
        @NotNull PrefetchList prefetch,
        @NotNull Project project,
        @NotNull String path,
        @NotNull Resource resource,
        @NotNull Map<String, Integer> fileIndexLookup
    ) {
        final int index = fileIndexLookup.get(path);
        final byte[] data;

        try {
            data = resource.readAllBytes();
        } catch (IOException e) {
            log.warn("Unable to read '{}': {}", path, e.getMessage());
            return;
        }

        if (data.length != prefetch.sizes()[index]) {
            log.warn("Size mismatch for '{}' ({}), updating to match the actual size ({})", path, prefetch.sizes()[index], data.length);
            prefetch.sizes()[index] = data.length;
        }

        final Set<String> references = new HashSet<>();
        final RTTICoreFile file;

        try {
            file = project.getCoreFileReader().read(new ByteArrayInputStream(data), ThrowingErrorHandlingStrategy.getInstance());
        } catch (Exception e) {
            log.warn("Unable to read core binary '{}': {}", path, e.getMessage());
            return;
        }

        file.visitAllObjects(RTTIReference.External.class, ref -> {
            if (ref.kind() == RTTIReference.Kind.LINK) {
                references.add(ref.path());
            }
        });

        final int[] oldLinks = IntStream.of(prefetch.links()[index]).sorted().toArray();
        final int[] newLinks = references.stream()
            .map(ref -> Objects.requireNonNull(fileIndexLookup.get(ref), () -> "Can't find '" + ref + "'"))
            .mapToInt(Integer::intValue)
            .sorted()
            .toArray();

        if (!Arrays.equals(oldLinks, newLinks)) {
            log.warn("Links mismatch for '{}':", path);

            log.warn("Prefetch links ({}):", Arrays.toString(oldLinks));
            for (int link : oldLinks) {
                log.warn(" - {}", prefetch.files()[link]);
            }

            log.warn("Actual links ({}):", Arrays.toString(newLinks));
            for (int link : newLinks) {
                log.warn(" - {}", prefetch.files()[link]);
            }

            prefetch.links()[index] = newLinks;
        }
    }

    /**
     * A supplier that determines whether a file should be updated
     * in the prefetch list or not by either returning a resource or {@code null}.
     *
     * @see #ofAll(PackfileManager)
     * @see #ofAll(Collection, PackfileManager)
     * @see #ofChanged(PackfileManager)
     * @see #ofChanged(Collection)
     */
    public interface FileSupplier {
        @Nullable
        Resource get(long hash) throws IOException;

        /**
         * Returns a supplier that returns a resource for any file in the packfile manager.
         */
        @NotNull
        static FileSupplier ofAll(@NotNull PackfileManager packfileManager) {
            return hash -> {
                final Packfile packfile = packfileManager.findFirst(hash);
                if (packfile == null) {
                    log.error("Can't find packfile for hash {}", hash);
                    return null;
                }
                final Packfile.FileEntry entry = packfile.getFileEntry(hash);
                if (entry == null) {
                    log.error("Can't find file entry for hash {}", hash);
                    return null;
                }
                return new PackfileResource(packfile, entry);
            };
        }

        /**
         * Returns a supplier that returns a resource only for changed files in the packfile manager
         */
        @NotNull
        static FileSupplier ofChanged(@NotNull PackfileManager packfileManager) {
            final Map<Long, Change> files = packfileManager.getArchives().stream()
                .filter(Packfile::hasChanges)
                .flatMap(packfile -> packfile.getChanges().entrySet().stream())
                .collect(Collectors.toMap(
                    x -> x.getKey().hash(),
                    Map.Entry::getValue
                ));

            return hash -> {
                final Change change = files.get(hash);
                return change != null ? change.toResource() : null;
            };
        }

        /**
         * Returns a supplier that returns a resource for any file in the packfile writer.
         * <p>
         * If the file is not found, it will try to find it in the packfile manager.
         */
        @NotNull
        static FileSupplier ofAll(@NotNull Collection<Change> changes, @NotNull PackfileManager packfileManager) {
            final FileSupplier ofAll = ofAll(packfileManager);
            final FileSupplier ofChanged = ofChanged(changes);

            return hash -> {
                final Resource resource = ofChanged.get(hash);
                return resource != null ? resource : ofAll.get(hash);
            };
        }

        /**
         * Returns a supplier that returns a resource for any file in the packfile writer.
         */
        @NotNull
        static FileSupplier ofChanged(@NotNull Collection<Change> changes) {
            final Map<Long, Change> files = changes.stream()
                .collect(Collectors.toMap(
                    Change::hash,
                    Function.identity()
                ));

            return hash -> {
                final Change change = files.get(hash);
                return change != null ? change.toResource() : null;
            };
        }
    }

    public record ChangeInfo(@NotNull Packfile packfile, @NotNull FilePath path, @NotNull Change change) {}
}
