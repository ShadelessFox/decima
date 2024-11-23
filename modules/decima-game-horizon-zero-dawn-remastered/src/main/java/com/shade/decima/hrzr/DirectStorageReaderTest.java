package com.shade.decima.hrzr;

import com.shade.decima.hrzr.rtti.CoreFileReader;
import com.shade.decima.hrzr.rtti.HRZRTypeFactory;
import com.shade.decima.hrzr.storage.PackFileAssetId;
import com.shade.decima.hrzr.storage.PackFileManager;
import com.shade.decima.hrzr.storage.PathResolver;
import com.shade.decima.rtti.TypeNotFoundException;
import com.shade.decima.storage.Asset;
import com.shade.decima.storage.AssetId;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;

import static com.shade.decima.rtti.HorizonZeroDawnRemastered.ERenderPlatform;

public class DirectStorageReaderTest {
    private static final Logger log = LoggerFactory.getLogger(DirectStorageReaderTest.class);

    public static void main(String[] args) throws IOException {
        var source = Path.of("E:/SteamLibrary/steamapps/common/Horizon Zero Dawn Remastered");
        var resolver = new HorizonPathResolver(source);

        log.info("Loading archives");
        try (var manager = new PackFileManager(resolver)) {
            var factory = new HRZRTypeFactory();
            var assets = new TreeMap<AssetId, Asset>();

            for (Asset asset : manager.assets()) {
                assets.put(asset.id(), asset);
            }

            log.info("Reading {} assets", assets.size());
            for (Asset asset : assets.tailMap(PackFileAssetId.ofHash(213440662289960006L)).values()) {
                var id = asset.id();
                var data = BinaryReader.wrap(manager.load(id));

                log.info("Reading {}", id);
                try (CoreFileReader reader = new CoreFileReader(data, factory)) {
                    try {
                        List<Object> objects = reader.read();
                        log.info("Read {} objects", objects.size());
                    } catch (TypeNotFoundException e) {
                        log.error("Unable to read: {}", e.getMessage());
                    }
                }
            }
        }
    }

    private record HorizonPathResolver(@NotNull Path source) implements PathResolver {
        @NotNull
        @Override
        public Path resolve(@NotNull String path) {
            String[] parts = path.split(":", 2);
            return switch (parts[0]) {
                case "source" -> source.resolve(parts[1]);
                case "cache" -> resolve("source:LocalCache" + ERenderPlatform._1).resolve(parts[1]);
                default -> throw new IllegalArgumentException("Unknown device path: " + path);
            };
        }
    }
}
