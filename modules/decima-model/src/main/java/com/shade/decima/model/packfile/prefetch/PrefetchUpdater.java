package com.shade.decima.model.packfile.prefetch;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.packfile.PackfileWriter;
import com.shade.decima.model.packfile.edit.Change;
import com.shade.decima.model.packfile.edit.MemoryChange;
import com.shade.decima.model.packfile.resource.Resource;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.FilePath;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PrefetchUpdater {
    private static final Logger log = LoggerFactory.getLogger(PrefetchUpdater.class);
    private static final String PREFETCH_PATH = "prefetch/fullgame.prefetch.core";

    private PrefetchUpdater() {
        // prevents instantiation
    }

    @Nullable
    public static ChangeInfo rebuildPrefetch(@NotNull ProgressMonitor monitor, @NotNull Project project, @NotNull FilePredicate predicate) throws IOException {
        final PackfileManager packfileManager = project.getPackfileManager();
        final RTTITypeRegistry typeRegistry = project.getTypeRegistry();

        final Packfile packfile = packfileManager.findFirst(PREFETCH_PATH);

        if (packfile == null) {
            log.error("Can't find prefetch file");
            return null;
        }

        final CoreBinary binary = CoreBinary.from(packfile.extract(PREFETCH_PATH), typeRegistry);

        if (binary.isEmpty()) {
            log.error("Prefetch file is empty");
            return null;
        }

        final RTTIObject object = binary.entries().get(0);
        final PrefetchList prefetch = PrefetchList.of(object);

        rebuildPrefetch(prefetch, monitor, packfileManager, typeRegistry, predicate);
        updatePrefetch(prefetch, object);

        final byte[] data = binary.serialize(typeRegistry);
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

    private static void rebuildPrefetch(@NotNull PrefetchList prefetch, @NotNull ProgressMonitor monitor, @NotNull PackfileManager manager, @NotNull RTTITypeRegistry registry, @NotNull FilePredicate predicate) {
        final Map<String, Integer> fileIndexLookup = new HashMap<>();
        for (int i = 0; i < prefetch.files().length; i++) {
            fileIndexLookup.put(prefetch.files()[i], i);
        }

        final String[] files = Arrays.stream(prefetch.files())
            .filter(file -> predicate.test(PackfileBase.getPathHash(PackfileBase.getNormalizedPath(file))))
            .toArray(String[]::new);

        try (ProgressMonitor.Task task = monitor.begin("Update prefetch", files.length)) {
            for (int i = 0; i < files.length; i++) {
                if (task.isCanceled()) {
                    return;
                }

                rebuildFile(prefetch, manager, registry, files[i], fileIndexLookup);

                if (i > 0 && i % 100 == 0) {
                    task.worked(100);
                }
            }
        }
    }

    private static void rebuildFile(
        @NotNull PrefetchList prefetch,
        @NotNull PackfileManager manager,
        @NotNull RTTITypeRegistry registry,
        @NotNull String path,
        @NotNull Map<String, Integer> fileIndexLookup
    ) {
        final int index = fileIndexLookup.get(path);
        final Packfile packfile = manager.findFirst(path);

        if (packfile == null) {
            log.warn("Can't find file {}", path);
            return;
        }

        final PackfileBase.FileEntry entry = Objects.requireNonNull(packfile.getFileEntry(path));
        final byte[] data;

        try {
            data = packfile.extract(entry.hash());
        } catch (Exception e) {
            log.warn("Unable to read '{}': {}", path, e.getMessage());
            return;
        }

        if (data.length != prefetch.sizes()[index]) {
            log.warn("Size mismatch for '{}' ({}), updating to match the actual size ({})", path, prefetch.sizes()[index], data.length);
            prefetch.sizes()[index] = data.length;
        }

        final Set<String> references = new HashSet<>();
        final CoreBinary binary;

        try {
            binary = CoreBinary.from(data, registry, false);
        } catch (Exception e) {
            log.warn("Unable to read core binary '{}': {}", path, e.getMessage());
            return;
        }

        binary.visitAllObjects(RTTIReference.External.class, ref -> {
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
     * A predicate that determines whether a file should be updated in the prefetch list or not.
     *
     * @see #ofAll()
     * @see #ofPackfileManager(PackfileManager)
     * @see #ofPackfileWriter(PackfileWriter)
     */
    public interface FilePredicate {
        boolean test(long hash);

        @NotNull
        static FilePredicate ofAll() {
            return hash -> true;
        }

        @NotNull
        static FilePredicate ofPackfileManager(@NotNull PackfileManager packfileManager) {
            final Set<Long> files = packfileManager.getPackfiles().stream()
                .filter(Packfile::hasChanges)
                .flatMap(packfile -> packfile.getChanges().keySet().stream())
                .map(FilePath::hash)
                .collect(Collectors.toSet());

            return files::contains;
        }

        @NotNull
        static FilePredicate ofPackfileWriter(@NotNull PackfileWriter packfileWriter) {
            final Set<Long> files = packfileWriter.getResources().stream()
                .map(Resource::hash)
                .collect(Collectors.toSet());

            return files::contains;
        }
    }

    public record ChangeInfo(@NotNull Packfile packfile, @NotNull FilePath path, @NotNull Change change) {}
}
