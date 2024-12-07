package com.shade.decima.game.hrzr.storage;

import com.shade.decima.game.Asset;
import com.shade.decima.game.AssetId;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import com.shade.util.io.BinaryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

public class PackFileManager implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(PackFileManager.class);

    private final NavigableSet<PackFileArchive> archives = new TreeSet<>();

    public PackFileManager(@NotNull PathResolver resolver) throws IOException {
        var root = resolver.resolve("cache:package");

        try (BinaryReader reader = BinaryReader.open(root.resolve("PackFileLocators.bin"))) {
            var count = reader.readInt();
            for (int i = 0; i < count; i++) {
                var info = PackFileInfo.read(reader);
                var path = root.resolve(info.name);
                mount(path, info);
            }
        }
    }

    private void mount(@NotNull Path path, @NotNull PackFileInfo info) throws IOException {
        if (Files.notExists(path)) {
            log.warn("Archive not found: {}", info.name);
            return;
        }
        archives.add(new PackFileArchive(path, info));
        log.info("Mounted file: {}", info.name);
    }

    @NotNull
    public List<? extends Asset> assets() {
        return archives.stream()
            .flatMap(archive -> archive.assets().stream())
            .distinct()
            .toList();
    }

    @NotNull
    public ByteBuffer load(@NotNull AssetId id) throws IOException {
        return archives.reversed().stream()
            .filter(archive -> archive.contains(id))
            .findFirst().orElseThrow()
            .load(id);
    }

    @Override
    public void close() throws IOException {
        for (PackFileArchive archive : archives) {
            archive.close();
        }
        archives.clear();
    }

    record PackFileAssetInfo(long hash, long offset, int length) {
        static PackFileAssetInfo read(@NotNull BinaryReader reader) throws IOException {
            var name = reader.readLong();
            var offset = reader.readInt();
            var length = reader.readInt();
            return new PackFileAssetInfo(name, Integer.toUnsignedLong(offset), length);
        }
    }

    record PackFileInfo(@NotNull String name, @NotNull PackFileAssetInfo[] assets) {
        static PackFileInfo read(@NotNull BinaryReader reader) throws IOException {
            var name = reader.readString(reader.readInt());
            var files = reader.readObjects(reader.readInt(), PackFileAssetInfo::read, PackFileAssetInfo[]::new);
            return new PackFileInfo(name, files);
        }
    }
}
