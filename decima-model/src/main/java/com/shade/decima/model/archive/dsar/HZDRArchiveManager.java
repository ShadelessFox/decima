package com.shade.decima.model.archive.dsar;

import com.shade.decima.model.archive.ArchiveFile;
import com.shade.decima.model.archive.ArchiveManager;
import com.shade.decima.model.archive.Archive;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.util.hash.MurmurHash3;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.shade.decima.model.packfile.Packfile.getNormalizedPath;

public final class HZDRArchiveManager implements ArchiveManager {
    private static final Logger log = LoggerFactory.getLogger(HZDRArchiveManager.class);
    private static final int MAX_ARCHIVES = 1 << 16;
    private static final int MAX_FILES_PER_ARCHIVE = 1 << 24;
    private static final int MAX_NAME_LENGTH = 1 << 16;

    private final List<DirectStorageArchive> archives = new ArrayList<>();
    private final Map<Long, ArchiveFile> files = new HashMap<>();

    public HZDRArchiveManager(@NotNull Path root) throws IOException {
        final Path locators = root.resolve("PackFileLocators.bin");
        final ByteBuffer buffer = ByteBuffer.wrap(Files.readAllBytes(locators)).order(ByteOrder.LITTLE_ENDIAN);
        final int archiveCount = readCount(buffer, MAX_ARCHIVES, "archive");

        try {
            for (int i = 0; i < archiveCount; i++) {
                final String name = readString(buffer);
                final int fileCount = readCount(buffer, MAX_FILES_PER_ARCHIVE, "file");
                final List<FileInfo> fileInfos = new ArrayList<>(fileCount);

                ensureRemaining(buffer, Math.multiplyExact((long) fileCount, 16));
                for (int j = 0; j < fileCount; j++) {
                    fileInfos.add(new FileInfo(
                        buffer.getLong(),
                        Integer.toUnsignedLong(buffer.getInt()),
                        Integer.toUnsignedLong(buffer.getInt())
                    ));
                }

                final Path path = root.resolve(name);
                if (Files.notExists(path)) {
                    log.warn("Archive not found: {}", path);
                    continue;
                }

                archives.add(new DirectStorageArchive(this, path, name, fileInfos));
            }

            if (buffer.hasRemaining()) {
                log.warn("Unexpected trailing data in '{}': {} bytes", locators, buffer.remaining());
            }

            archives.sort(Comparator.naturalOrder());
            for (DirectStorageArchive archive : archives.reversed()) {
                for (ArchiveFile file : archive.getFiles()) {
                    files.putIfAbsent(file.getIdentifier(), file);
                }
            }
        } catch (Throwable e) {
            try {
                close();
            } catch (Throwable suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
    }

    @Override
    public long getPathHash(@NotNull String path) {
        return hashPath(path);
    }

    public static long hashPath(@NotNull String path) {
        return Packfile.getPathHash(getNormalizedPath(path));
    }

    @Nullable
    @Override
    public ArchiveFile findFile(@NotNull String identifier) {
        return findFile(hashPath(identifier));
    }

    @Nullable
    @Override
    public ArchiveFile findFile(long identifier) {
        return files.get(identifier);
    }

    @NotNull
    @Override
    public Collection<? extends Archive> getArchives() {
        return List.copyOf(archives);
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;

        for (DirectStorageArchive archive : archives) {
            try {
                archive.close();
            } catch (IOException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }

        archives.clear();
        files.clear();

        if (failure != null) {
            throw failure;
        }
    }

    private static int readCount(@NotNull ByteBuffer buffer, int maximum, @NotNull String kind) throws IOException {
        ensureRemaining(buffer, Integer.BYTES);
        final int count = buffer.getInt();
        if (count < 0 || count > maximum) {
            throw new IOException("Invalid %s count in PackFileLocators.bin: %d".formatted(kind, count));
        }
        return count;
    }

    @NotNull
    private static String readString(@NotNull ByteBuffer buffer) throws IOException {
        final int length = readCount(buffer, MAX_NAME_LENGTH, "archive name byte");
        ensureRemaining(buffer, length);
        final byte[] data = new byte[length];
        buffer.get(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    private static void ensureRemaining(@NotNull ByteBuffer buffer, long length) throws EOFException {
        if (length < 0 || length > buffer.remaining()) {
            throw new EOFException("Unexpected end of PackFileLocators.bin");
        }
    }

    record FileInfo(long hash, long offset, long length) {}
}
