package com.shade.decima.game.hfw.game;

import com.shade.decima.game.FileSystem;
import com.shade.decima.game.Game;
import com.shade.decima.game.hfw.rtti.HFWTypeFactory;
import com.shade.decima.game.hfw.rtti.HFWTypeReader;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.EPlatform;
import com.shade.decima.game.hfw.storage.ObjectStreamingSystem;
import com.shade.decima.game.hfw.storage.StorageReadDevice;
import com.shade.decima.game.hfw.storage.StreamingGraphResource;
import com.shade.decima.game.hfw.storage.StreamingObjectReader;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public final class ForbiddenWestGame implements Game {
    private static final Logger log = LoggerFactory.getLogger(ForbiddenWestGame.class);

    private static final TypeFactory typeFactory;

    static {
        log.debug("Loading type factory");
        typeFactory = new HFWTypeFactory();
    }

    private final StreamingGraphResource streamingGraph;
    private final StorageReadDevice storageDevice;
    private final ObjectStreamingSystem streamingSystem;
    private final StreamingObjectReader streamingReader;

    public ForbiddenWestGame(Path source, EPlatform platform) throws IOException {
        var fileSystem = new ForbiddenWestFileSystem(source, platform);

        log.debug("Loading streaming graph");
        streamingGraph = readStreamingGraph(fileSystem, typeFactory);

        log.debug("Loading storage files");
        storageDevice = new StorageReadDevice(fileSystem);

        for (String file : streamingGraph.files()) {
            storageDevice.mount(file);
        }

        streamingSystem = new ObjectStreamingSystem(storageDevice, streamingGraph);
        streamingReader = new StreamingObjectReader(streamingSystem, typeFactory);
    }

    public StreamingGraphResource getStreamingGraph() {
        return streamingGraph;
    }

    public ObjectStreamingSystem getStreamingSystem() {
        return streamingSystem;
    }

    public StreamingObjectReader getStreamingReader() {
        return streamingReader;
    }

    @Override
    public void close() throws IOException {
        storageDevice.close();
    }

    private static StreamingGraphResource readStreamingGraph(FileSystem fileSystem, TypeFactory factory) throws IOException {
        try (var reader = BinaryReader.open(fileSystem.resolve("cache:package/streaming_graph.core"))) {
            var result = new HFWTypeReader().readObject(reader, factory);
            return new StreamingGraphResource((HorizonForbiddenWest.StreamingGraphResource) result.object(), factory);
        }
    }

    private record ForbiddenWestFileSystem(Path source, EPlatform platform) implements FileSystem {
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
