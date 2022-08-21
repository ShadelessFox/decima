package com.shade.decima.ui.navigator.impl;

import com.shade.util.NotNull;

import java.util.Arrays;
import java.util.Objects;

public record FilePath(@NotNull String[] parts, long hash) implements Comparable<FilePath> {
    public static final FilePath EMPTY_PATH = new FilePath(new String[0]);

    public FilePath(@NotNull String[] parts) {
        this(parts, 0);
    }

    @NotNull
    public FilePath concat(@NotNull String... other) {
        final String[] result = new String[parts.length + other.length];
        System.arraycopy(parts, 0, result, 0, parts.length);
        System.arraycopy(other, 0, result, parts.length, other.length);

        return new FilePath(result);
    }

    @NotNull
    public FilePath slice(int length) {
        final String[] result = new String[length];
        System.arraycopy(parts, 0, result, 0, length);

        return new FilePath(result);
    }

    public int length() {
        return parts.length;
    }

    @NotNull
    public String last() {
        return parts[parts.length - 1];
    }

    @NotNull
    public String full() {
        return String.join("/", parts);
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
        return hash == filePath.hash && Arrays.equals(parts, filePath.parts);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(hash);
        result = 31 * result + Arrays.hashCode(parts);
        return result;
    }

    @Override
    public String toString() {
        return "FilePath[path=" + full() + ", hash=" + hash + "]";
    }
}
