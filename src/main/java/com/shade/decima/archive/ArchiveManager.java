package com.shade.decima.archive;

import com.shade.decima.util.Compressor;
import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.objects.RTTIObject;
import com.shade.decima.rtti.registry.RTTITypeRegistry;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;
import com.shade.decima.util.hash.MurmurHash3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;

public class ArchiveManager implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(ArchiveManager.class);
    private static final ArchiveManager instance = new ArchiveManager();

    private final Map<Long, Archive> hashToArchive;
    private final Map<Long, Archive.FileEntry> hashToFile;
    private final Map<String, String> hashToName;

    public ArchiveManager() {
        this.hashToArchive = new HashMap<>();
        this.hashToFile = new HashMap<>();
        this.hashToName = new HashMap<>();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("ds_archives.yaml")) {
            hashToName.putAll(new Yaml().load(is));
        } catch (IOException e) {
            log.warn("Can't load archive name mappings", e);
        }
    }

    @NotNull
    public static ArchiveManager getInstance() {
        return instance;
    }

    public void load(@NotNull Path path) throws IOException {
        final String name = sanitizeArchiveName(path);
        final Archive archive = new Archive(path, hashToName.getOrDefault(name, name));

        hashToArchive.put(Long.reverseBytes(Long.parseUnsignedLong(name, 0, 16, 16)), archive);

        for (Archive.FileEntry entry : archive.getFileEntries()) {
            hashToFile.put(entry.hash(), entry);
        }
    }

    @NotNull
    public List<RTTIObject> readFileObjects(@NotNull Compressor compressor, @NotNull String path) throws IOException {
        final Archive.FileEntry entry = getFileEntry(path);

        if (entry == null) {
            throw new IllegalArgumentException("Can't find file " + path);
        }

        return readFileObjects(compressor, entry);
    }

    @NotNull
    public List<RTTIObject> readFileObjects(@NotNull Compressor compressor, @NotNull Archive.FileEntry entry) throws IOException {
        final ByteBuffer buffer = ByteBuffer
            .wrap(entry.archive().unpack(compressor, entry))
            .order(ByteOrder.LITTLE_ENDIAN);

        final List<RTTIObject> objects = new ArrayList<>();

        while (buffer.remaining() > 0) {
            final long id = buffer.getLong();
            final int size = buffer.getInt();

            final RTTIType<?> type = RTTITypeRegistry.getInstance().find(id);
            final RTTIObject data = (RTTIObject) type.read(buffer.slice(buffer.position(), size).order(ByteOrder.LITTLE_ENDIAN));

            objects.add(data);

            buffer.position(buffer.position() + size);
        }

        return objects;
    }

    @Nullable
    public Archive.FileEntry getFileEntry(@NotNull String path) {
        return getFileEntry(hashFileName(sanitizeFileName(path)));
    }

    @Nullable
    public Archive.FileEntry getFileEntry(long hash) {
        return hashToFile.get(hash);
    }

    @Nullable
    public Archive getArchive(@NotNull String name) {
        return getArchive(hashArchiveName(sanitizeArchiveName(name)));
    }

    @Nullable
    public Archive getArchive(long hash) {
        return hashToArchive.get(hash);
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
        for (Archive archive : hashToArchive.values()) {
            archive.close();
        }
    }
}
