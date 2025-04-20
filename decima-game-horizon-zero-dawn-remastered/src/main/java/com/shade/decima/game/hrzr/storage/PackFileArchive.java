package com.shade.decima.game.hrzr.storage;

import com.shade.decima.game.Archive;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;
import com.shade.util.io.DirectStorageReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PackFileArchive implements Archive<PackFileAssetId, PackFileAsset>, Comparable<PackFileArchive> {
    private final Path path;
    private final String name;
    private final BinaryReader reader;
    private final Map<PackFileAssetId, PackFileAsset> assets;

    PackFileArchive(@NotNull Path path, @NotNull PackFileManager.PackFileInfo info) throws IOException {
        this.path = path;
        this.name = info.name();
        this.reader = DirectStorageReader.open(path);
        this.assets = new HashMap<>(info.assets().length);

        for (PackFileManager.PackFileAssetInfo asset : info.assets()) {
            PackFileAssetId id = PackFileAssetId.ofHash(asset.hash());
            assets.put(id, new PackFileAsset(id, asset.offset(), asset.length()));
        }
    }

    @NotNull
    @Override
    public Optional<PackFileAsset> get(@NotNull PackFileAssetId id) {
        return Optional.ofNullable(assets.get(id));
    }

    @NotNull
    @Override
    public byte[] read(@NotNull PackFileAssetId id) throws IOException {
        var asset = get(id).orElseThrow(FileNotFoundException::new);
        var buffer = ByteBuffer.allocate(asset.length()).order(ByteOrder.LITTLE_ENDIAN);
        synchronized (reader) {
            reader.position(asset.offset());
            reader.readBytes(buffer.array(), 0, buffer.limit());
        }
        return buffer.array();
    }

    @NotNull
    @Override
    public List<PackFileAsset> assets() {
        return List.copyOf(assets.values());
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public int compareTo(@NotNull PackFileArchive o) {
        return name.compareToIgnoreCase(o.name);
    }

    @Override
    public String toString() {
        return path.toString();
    }
}
