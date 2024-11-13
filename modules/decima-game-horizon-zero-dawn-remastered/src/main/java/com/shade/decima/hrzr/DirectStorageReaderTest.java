package com.shade.decima.hrzr;

import com.shade.decima.hrzr.rtti.CoreFileReader;
import com.shade.decima.hrzr.rtti.HRZRTypeFactory;
import com.shade.decima.hrzr.storage.PackFileAssetId;
import com.shade.decima.hrzr.storage.PackFileManager;
import com.shade.decima.rtti.PathResolver;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class DirectStorageReaderTest {
    private static final Logger log = LoggerFactory.getLogger(DirectStorageReaderTest.class);

    public static void main(String[] args) throws IOException {
        var source = Path.of("E:/SteamLibrary/steamapps/common/Horizon Zero Dawn Remastered");
        var resolver = new HorizonPathResolver(source);

        log.info("Loading archives");
        try (var manager = new PackFileManager(resolver)) {
            var id = PackFileAssetId.ofPath("prefetch/fullgame.prefetch.core");
            var data = BinaryReader.wrap(manager.load(id));

            log.info("Loading type factory");
            var factory = new HRZRTypeFactory();

            log.info("Reading prefetch");
            try (CoreFileReader reader = new CoreFileReader(data, factory)) {
                List<Object> objects = reader.read();
                log.info("Read {} objects", objects.size());
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
                case "cache" -> resolve("source:LocalCacheDX12/" + parts[1]);
                default -> throw new IllegalArgumentException("Unknown device path: " + path);
            };
        }
    }
}
