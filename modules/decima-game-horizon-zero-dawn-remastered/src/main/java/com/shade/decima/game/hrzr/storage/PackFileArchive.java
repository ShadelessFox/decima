package com.shade.decima.game.hrzr.storage;

import com.shade.decima.game.Asset;
import com.shade.decima.game.hrzr.DirectStorageReader;
import com.shade.decima.game.Archive;
import com.shade.decima.game.AssetId;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackFileArchive implements Archive, Comparable<PackFileArchive> {
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
    public PackFileAsset get(@NotNull AssetId id) throws IOException {
        PackFileAsset asset = assets.get((PackFileAssetId) id);
        if (asset == null) {
            throw new IllegalArgumentException("Asset not found: " + id);
        }
        return asset;
    }

    @NotNull
    @Override
    public ByteBuffer load(@NotNull AssetId id) throws IOException {
        PackFileAsset asset = get(id);
        ByteBuffer buffer = ByteBuffer.allocate(asset.length()).order(ByteOrder.LITTLE_ENDIAN);
        synchronized (reader) {
            reader.position(asset.offset());
            reader.readBytes(buffer.array(), 0, buffer.limit());
        }
        return buffer;
    }

    @Override
    public boolean contains(@NotNull AssetId id) {
        return assets.containsKey((PackFileAssetId) id);
    }

    @NotNull
    @Override
    public List<PackFileAsset> assets() {
        return List.copyOf(assets.values());
    }

    @NotNull
    @Override
    public String name() {
        return name;
    }

    @NotNull
    @Override
    public Path path() {
        return path;
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
        return name;
    }
}
