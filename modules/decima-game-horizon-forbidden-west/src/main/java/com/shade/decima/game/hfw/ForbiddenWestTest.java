package com.shade.decima.game.hfw;

import com.shade.decima.game.hfw.rtti.HFWTypeFactory;
import com.shade.decima.game.hfw.rtti.HFWTypeReader;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest;
import com.shade.decima.game.hfw.storage.*;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.nio.file.Path;

import static com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.EPlatform;

public class ForbiddenWestTest {
    public static void main(String[] args) throws IOException {
        var source = Path.of("E:/SteamLibrary/steamapps/common/Horizon Forbidden West Complete Edition");
        var resolver = new HorizonPathResolver(source);
        var factory = new HFWTypeFactory();

        StreamingGraphResource graph;

        try (var reader = BinaryReader.open(resolver.resolve("cache:package/streaming_graph.core"))) {
            var object = new HFWTypeReader().readObject(reader, factory).object();
            graph = new StreamingGraphResource((HorizonForbiddenWest.StreamingGraphResource) object, factory);
        }

        try (StorageReadDevice device = new StorageReadDevice(resolver)) {
            for (String file : graph.files()) {
                device.mount(file);
            }

            ObjectStreamingSystem system = new ObjectStreamingSystem(device, graph);
            StreamingObjectReader reader = new StreamingObjectReader(system, factory);

            HFWTypeReader.ObjectInfo result = reader.readObject("00377119-c8e7-45d7-b37d-0f6e240c3116");
            System.out.println(result);
        }
    }

    private record HorizonPathResolver(@NotNull Path source) implements PathResolver {
        @NotNull
        @Override
        public Path resolve(@NotNull String path) {
            String[] parts = path.split(":", 2);
            return switch (parts[0]) {
                case "source" -> source.resolve(parts[1]);
                case "cache" -> resolve("source:LocalCache" + EPlatform.WinGame).resolve(parts[1]);
                default -> throw new IllegalArgumentException("Unknown device path: " + path);
            };
        }
    }
}
