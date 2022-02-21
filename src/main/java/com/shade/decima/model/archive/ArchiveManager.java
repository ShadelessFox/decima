package com.shade.decima.model.archive;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.Compressor;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.model.util.hash.MurmurHash3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ArchiveManager implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(ArchiveManager.class);

    private final Set<Archive> archives;
    private final Map<String, ArchiveInfo> nameToArchiveInfo;
    private final Map<Long, Archive.FileEntry> hashToFile;
    private final RTTITypeRegistry registry;

    public ArchiveManager(@NotNull RTTITypeRegistry registry, @Nullable Path info) {
        this.registry = registry;
        this.archives = new HashSet<>();
        this.hashToFile = new HashMap<>();
        this.nameToArchiveInfo = new HashMap<>();

        if (info != null) {
            try (BufferedReader reader = Files.newBufferedReader(info)) {
                nameToArchiveInfo.putAll(new Gson().fromJson(reader, new TypeToken<Map<String, ArchiveInfo>>() {}.getType()));
            } catch (IOException e) {
                log.warn("Can't load archive name mappings", e);
            }
        }
    }

    public void load(@NotNull Path path) throws IOException {
        String name = path.getFileName().toString();

        if (name.indexOf('.') >= 0) {
            name = name.substring(0, name.indexOf('.'));
        }

        if (nameToArchiveInfo.containsKey(name)) {
            name = nameToArchiveInfo.get(name).name;
        }

        final Archive archive = new Archive(path, name);

        archives.add(archive);

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

            try {
                final RTTIType<?> type = registry.find(id);
                final RTTIObject data = (RTTIObject) type.read(registry, buffer.slice(buffer.position(), size).order(ByteOrder.LITTLE_ENDIAN));

                objects.add(data);
            } catch (Exception e) {
                log.error("Can't read core object", e);
            }

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

    public static long hashFileName(@NotNull String path) {
        final byte[] bytes = path.getBytes();
        final byte[] buffer = Arrays.copyOf(bytes, bytes.length + 1);
        buffer[bytes.length] = 0;
        return MurmurHash3.mmh3(buffer, 0, buffer.length)[0];
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

    @Override
    public void close() throws IOException {
        for (Archive archive : archives) {
            archive.close();
        }
    }

    public static class ArchiveInfo {
        private final String id;
        private final String name;

        public ArchiveInfo(@NotNull String id, @NotNull String name) {
            this.id = id;
            this.name = name;
        }
    }
}
