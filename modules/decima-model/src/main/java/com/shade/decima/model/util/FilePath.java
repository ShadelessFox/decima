package com.shade.decima.model.util;

import com.shade.decima.model.packfile.Packfile;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.util.Arrays;
import java.util.Objects;

public record FilePath(@NotNull String[] parts, long hash) implements Comparable<FilePath> {
    public static final FilePath EMPTY_PATH = new FilePath(new String[0]);

    public FilePath(@NotNull String[] parts) {
        this(parts, 0);
    }

    @NotNull
    public static FilePath of(@NotNull String path) {
        return of(path, false);
    }

    @NotNull
    public static FilePath of(@NotNull String path, boolean computeHash) {
        return of(path, computeHash, true);
    }

    @NotNull
    public static FilePath of(@NotNull String path, boolean computeHash, boolean normalize) {
        if (normalize) {
            path = Packfile.getNormalizedPath(path);
        }
        if (computeHash) {
            return new FilePath(path.split("/"), Packfile.getPathHash(path));
        } else {
            return new FilePath(path.split("/"));
        }
    }

    @NotNull
    public FilePath concat(@NotNull String... other) {
        final String[] result = new String[parts.length + other.length];
        System.arraycopy(parts, 0, result, 0, parts.length);
        System.arraycopy(other, 0, result, parts.length, other.length);

        return new FilePath(result);
    }

    @NotNull
    public FilePath subpath(int beginIndex) {
        return subpath(beginIndex, length());
    }

    @NotNull
    public FilePath subpath(int beginIndex, int endIndex) {
        Objects.checkFromToIndex(beginIndex, endIndex, length());

        if (beginIndex == endIndex) {
            return EMPTY_PATH;
        } else if (beginIndex == 0 && endIndex == length()) {
            return this;
        } else {
            final String[] result = new String[endIndex - beginIndex];
            System.arraycopy(parts, beginIndex, result, 0, result.length);
            return new FilePath(result);
        }
    }

    @NotNull
    public FilePath parent() {
        return subpath(0, length() - 1);
    }

    public int length() {
        return parts.length;
    }

    @NotNull
    public String name(int index) {
        return parts[index];
    }

    @NotNull
    public String first() {
        return parts[0];
    }

    @NotNull
    public String last() {
        return parts[parts.length - 1];
    }

    @NotNull
    public String full() {
        return String.join("/", parts);
    }

    public boolean startsWith(@NotNull FilePath other) {
        return IOUtils.startsWith(parts, other.parts);
    }

    @Override
    public int compareTo(FilePath o) {
        int length = Math.min(parts.length, o.parts.length);

        for (int i = 0; i < length; i++) {
            final String a = parts[i];
            final String b = o.parts[i];

            if (a.equals("*")) {
                return 1;
            }

            if (b.equals("*")) {
                return -1;
            }

            final int value = a.compareTo(b);

            if (value != 0) {
                return value;
            }
        }

        return parts.length - o.parts.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilePath filePath = (FilePath) o;
        return Arrays.equals(parts, filePath.parts);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(parts);
    }

    @Override
    public String toString() {
        return "FilePath[path=" + full() + ", hash=" + hash + "]";
    }
}
