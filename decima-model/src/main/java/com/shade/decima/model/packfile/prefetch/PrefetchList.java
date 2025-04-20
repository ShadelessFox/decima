package com.shade.decima.model.packfile.prefetch;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.util.NotNull;

public record PrefetchList(@NotNull String[] files, int[] sizes, int[][] links) {
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
}
