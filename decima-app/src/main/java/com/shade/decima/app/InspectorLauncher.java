package com.shade.decima.app;

import com.shade.decima.app.ui.GraphInspector;
import com.shade.decima.game.FileSystem;
import com.shade.decima.game.hfw.rtti.HFWTypeFactory;
import com.shade.decima.game.hfw.rtti.HFWTypeReader;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.EPlatform;
import com.shade.decima.game.hfw.storage.ObjectStreamingSystem;
import com.shade.decima.game.hfw.storage.StorageReadDevice;
import com.shade.decima.game.hfw.storage.StreamingGraphResource;
import com.shade.decima.game.hfw.storage.StreamingObjectReader;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;

public class InspectorLauncher {
    private static final Logger log = LoggerFactory.getLogger(InspectorLauncher.class);

    public static void main(String[] args) throws Exception {
        var path = Path.of("E:/SteamLibrary/steamapps/common/Horizon Forbidden West Complete Edition");

        log.info("Loading type factory");
        var fileSystem = new HorizonFileSystem(path);
        var factory = new HFWTypeFactory();

        log.info("Loading streaming graph");
        var graph = readStreamingGraph(fileSystem, factory);

        log.info("Loading storage files");
        try (StorageReadDevice device = new StorageReadDevice(fileSystem)) {
            for (String file : graph.files()) {
                device.mount(file);
            }

            ObjectStreamingSystem system = new ObjectStreamingSystem(device, graph);
            StreamingObjectReader reader = new StreamingObjectReader(system, factory);

            log.info("Opening UI");
            ui(graph, reader);
        }
    }

    private static void ui(StreamingGraphResource graph, StreamingObjectReader reader) throws Exception {
        UIManager.getDefaults().addResourceBundle("com.shade.decima.app.ui.util.Bundle");
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        SwingUtilities.invokeAndWait(() -> GraphInspector.show(graph, reader));
    }

    @NotNull
    private static StreamingGraphResource readStreamingGraph(@NotNull FileSystem fileSystem, @NotNull HFWTypeFactory factory) throws IOException {
        try (var reader = BinaryReader.open(fileSystem.resolve("cache:package/streaming_graph.core"))) {
            var result = new HFWTypeReader().readObject(reader, factory);
            return new StreamingGraphResource((HorizonForbiddenWest.StreamingGraphResource) result.object(), factory);
        }
    }

    private record HorizonFileSystem(@NotNull Path source) implements FileSystem {
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
