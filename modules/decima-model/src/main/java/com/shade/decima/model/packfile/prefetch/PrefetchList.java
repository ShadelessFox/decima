package com.shade.decima.model.packfile.prefetch;

import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.FilePath;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record PrefetchList(@NotNull String[] files, int[] sizes, int[][] links) {
    private static final Logger log = LoggerFactory.getLogger(PrefetchList.class);

    @NotNull
    public static PrefetchList of(@NotNull RTTIObject prefetch) {
        final var prefetchFiles = prefetch.objs("Files");
        final var prefetchLinks = prefetch.<int[]>get("Links");
        final var prefetchSizes = prefetch.<int[]>get("Sizes");

        final var files = new String[prefetchFiles.length];
        final var links = new int[prefetchFiles.length][];

        for (int i = 0, j = 0; i < prefetchFiles.length; i++, j++) {
            final int count = prefetchLinks[j];
            final var current = links[i] = new int[count];

            files[i] = prefetchFiles[i].str("Path");
            System.arraycopy(prefetchLinks, j + 1, current, 0, count);

            j += count;
        }

        return new PrefetchList(files, prefetchSizes, links);
    }

    public void update(@NotNull RTTIObject prefetch) {
        final int count = Arrays.stream(links).mapToInt(x -> x.length).sum() + links.length;
        final var result = new int[count];

        for (int i = 0, j = 0; i < links.length; i++, j++) {
            final int[] src = links[i];
            result[j] = src.length;
            System.arraycopy(src, 0, result, j + 1, src.length);
            j += src.length;
        }

        prefetch.set("Links", result);
        prefetch.set("Sizes", sizes);
    }

    public void rebuild(@NotNull ProgressMonitor monitor, @NotNull PackfileManager manager, @NotNull RTTITypeRegistry registry, boolean changedFilesOnly) {
        final Map<String, Integer> fileIndexLookup = new HashMap<>();
        for (int i = 0; i < files.length; i++) {
            fileIndexLookup.put(files[i], i);
        }

        final String[] files;

        if (changedFilesOnly) {
            final Set<Long> changedFiles = manager.getPackfiles().stream()
                .filter(Packfile::hasChanges)
                .flatMap(packfile -> packfile.getChanges().keySet().stream())
                .map(FilePath::hash)
                .collect(Collectors.toSet());

            files = Arrays.stream(this.files)
                .filter(file -> changedFiles.contains(PackfileBase.getPathHash(PackfileBase.getNormalizedPath(file))))
                .toArray(String[]::new);
        } else {
            files = this.files;
        }

        try (ProgressMonitor.Task task = monitor.begin("Update prefetch", files.length)) {
            for (int i = 0; i < files.length; i++) {
                if (task.isCanceled()) {
                    return;
                }

                rebuildFile(manager, registry, files[i], fileIndexLookup);

                if (i > 0 && i % 100 == 0) {
                    task.worked(100);
                }
            }
        }
    }

    private void rebuildFile(@NotNull PackfileManager manager, @NotNull RTTITypeRegistry registry, @NotNull String path, @NotNull Map<String, Integer> fileIndexLookup) {
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

        if (data.length != sizes[index]) {
            log.warn("Size mismatch for '{}' ({}), updating to match the actual size ({})", path, sizes[index], data.length);
            sizes[index] = data.length;
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

        final int[] oldLinks = IntStream.of(links[index]).sorted().toArray();
        final int[] newLinks = references.stream()
            .map(ref -> Objects.requireNonNull(fileIndexLookup.get(ref), () -> "Can't find '" + ref + "'"))
            .mapToInt(Integer::intValue)
            .sorted()
            .toArray();

        if (!Arrays.equals(oldLinks, newLinks)) {
            log.warn("Links mismatch for '{}':", path);

            log.warn("Prefetch links ({}):", Arrays.toString(oldLinks));
            for (int link : oldLinks) {
                log.warn(" - {}", this.files[link]);
            }

            log.warn("Actual links ({}):", Arrays.toString(newLinks));
            for (int link : newLinks) {
                log.warn(" - {}", this.files[link]);
            }

            links[index] = newLinks;
        }
    }
}
