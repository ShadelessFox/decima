package com.shade.decima.game.hrzr.storage;

import com.shade.decima.game.FileSystem;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

public class PackFileManager implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(PackFileManager.class);

    private final NavigableSet<PackFileArchive> archives = new TreeSet<>();
    private final Map<PackFileAssetId, PackFileArchive> assets = new HashMap<>();

    public PackFileManager(@NotNull FileSystem fileSystem) throws IOException {
        var root = fileSystem.resolve("cache:package");

        try (BinaryReader reader = BinaryReader.open(root.resolve("PackFileLocators.bin"))) {
            var count = reader.readInt();
            for (int i = 0; i < count; i++) {
                var info = PackFileInfo.read(reader);
                var path = root.resolve(info.name);
                mount(path, info);
            }
        }

        for (PackFileArchive archive : archives.reversed()) {
            for (PackFileAsset asset : archive.assets()) {
                assets.putIfAbsent(asset.id(), archive);
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
    public byte[] load(@NotNull PackFileAssetId id) throws IOException {
        var archive = assets.get(id);
        if (archive == null) {
            throw new FileNotFoundException("Asset not found: " + id);
        }
        return archive.read(id);
    }

    @Override
    public void close() throws IOException {
        for (PackFileArchive archive : archives) {
            archive.close();
        }
        archives.clear();
        assets.clear();
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
