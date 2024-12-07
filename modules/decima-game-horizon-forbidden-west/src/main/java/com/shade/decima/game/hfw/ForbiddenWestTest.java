package com.shade.decima.game.hfw;

import com.shade.decima.game.hfw.rtti.HFWTypeFactory;
import com.shade.decima.game.hfw.rtti.RTTIBinaryReader;
import com.shade.decima.game.hfw.storage.PathResolver;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.nio.file.Path;

import static com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.*;

public class ForbiddenWestTest {
    public static void main(String[] args) throws IOException {
        var source = Path.of("E:/SteamLibrary/steamapps/common/Horizon Forbidden West Complete Edition");
        var resolver = new HorizonPathResolver(source);
        var factory = new HFWTypeFactory();

        StreamingGraphResource graph;

        try (var reader = new RTTIBinaryReader(BinaryReader.open(resolver.resolve("cache:package/streaming_graph.core")), factory)) {
            graph = (StreamingGraphResource) reader.readObject().object();
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
