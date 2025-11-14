package com.shade.decima.game.until_dawn.game;

import com.shade.decima.game.FileSystem;
import com.shade.decima.game.Game;
import com.shade.decima.game.until_dawn.rtti.UntilDawn.Asset;
import com.shade.decima.game.until_dawn.rtti.UntilDawn.EPlatform;
import com.shade.decima.game.until_dawn.rtti.UntilDawnTypeFactory;
import com.shade.decima.game.until_dawn.rtti.UntilDawnTypeReader;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.CompressedBinaryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class UntilDawnGame implements Game {
    private static final Logger log = LoggerFactory.getLogger(UntilDawnGame.class);

    private static final TypeFactory typeFactory;
    private final FileSystem fileSystem;

    static {
        log.debug("Loading type factory");
        typeFactory = new UntilDawnTypeFactory();
    }

    public UntilDawnGame(Path source, EPlatform platform) {
        this.fileSystem = new UntilDawnFileSystem(source, platform);
    }

    public Path getLumps() {
        return fileSystem.resolve("cache:lumps");
    }

    public Asset loadRootAsset(String location) throws IOException {
        var objectSystem = loadObjectSystem(location);
        return objectSystem.stream()
            .filter(Asset.class::isInstance).map(Asset.class::cast)
            .filter(asset -> asset.isRootAsset() && asset.location().equalsIgnoreCase(location))
            .findFirst().orElseThrow();
    }

    public List<Object> loadObjectSystem(String lumpLocation) throws IOException {
        try (var reader = CompressedBinaryReader.open(fileSystem.resolve(lumpLocation))) {
            return new UntilDawnTypeReader().read(reader, typeFactory);
        }
    }

    public String pathForLumpLocation(String lumpLocation) {
        return "cache:lumps/" + lumpLocation + ".core";
    }

    @Override
    public void close() {
        // nothing to close, yet
    }

    private record UntilDawnFileSystem(Path source, EPlatform platform) implements FileSystem {
        @NotNull
        @Override
        public Path resolve(@NotNull String path) {
            String[] parts = path.split(":", 2);
            return switch (parts[0]) {
                case "source" -> source.resolve(parts[1]);
                case "cache" -> resolve("source:LocalCache" + platform).resolve(parts[1]);
                default -> throw new IllegalArgumentException("Unknown device path: " + path);
            };
        }
    }
}
