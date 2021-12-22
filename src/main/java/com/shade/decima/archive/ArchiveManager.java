package com.shade.decima.archive;

import com.shade.decima.util.hash.MurmurHash3;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class ArchiveManager implements Closeable {
    private final Set<Archive> archives;
    private final Map<Long, Archive> archiveLookup;
    private final Map<Long, Archive.FileEntry> archiveFileEntryLookup;

    public ArchiveManager() {
        this.archives = new HashSet<>();
        this.archiveLookup = new HashMap<>();
        this.archiveFileEntryLookup = new HashMap<>();
    }

    public void load(@NotNull Path path) throws IOException {
        final Archive archive = new Archive(path);

        archives.add(archive);
        archiveLookup.put(Long.reverseBytes(Long.parseUnsignedLong(sanitizeArchiveName(path), 0, 16, 16)), archive);

        for (Archive.FileEntry entry : archive.getFileEntries()) {
            archiveFileEntryLookup.put(entry.hash(), entry);
        }
    }

    @Nullable
    public Archive.FileEntry getFileEntry(@NotNull String path) {
        return getFileEntry(hashFileName(sanitizeFileName(path)));
    }

    @Nullable
    public Archive.FileEntry getFileEntry(long hash) {
        return archiveFileEntryLookup.get(hash);
    }

    @Nullable
    public Archive getArchive(@NotNull String name) {
        return getArchive(hashArchiveName(sanitizeArchiveName(name)));
    }

    @Nullable
    public Archive getArchive(long hash) {
        return archiveLookup.get(hash);
    }

    private static long hashFileName(@NotNull String path) {
        final byte[] bytes = path.getBytes();
        final byte[] buffer = Arrays.copyOf(bytes, bytes.length + 1);
        buffer[bytes.length] = 0;
        return MurmurHash3.mmh3(buffer, 0, buffer.length)[0];
    }

    private static long hashArchiveName(@NotNull String name) {
        final byte[] bytes = name.getBytes();
        return MurmurHash3.mmh3(bytes, 0, bytes.length)[0];
    }

    @NotNull
    private static String sanitizeFileName(@NotNull String path) {
        final String sanitized = path.replace('\\', '/');
        final String extension = sanitized.substring(sanitized.indexOf('.') + 1);
        if (!extension.equals("stream") && !extension.equals("core")) {
            return sanitized + ".core";
        }
        return sanitized;
    }

    @NotNull
    private static String sanitizeArchiveName(@NotNull Path path) {
        return sanitizeArchiveName(path.getFileName().toString());
    }

    @NotNull
    private static String sanitizeArchiveName(@NotNull String name) {
        if (name.indexOf('.') >= 0) {
            name = name.substring(0, name.indexOf('.'));
        }
        return name.toLowerCase();
    }

    @Override
    public void close() throws IOException {
        for (Archive archive : archives) {
            archive.close();
        }
    }
}
