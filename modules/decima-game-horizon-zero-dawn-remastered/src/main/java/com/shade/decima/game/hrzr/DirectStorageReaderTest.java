package com.shade.decima.game.hrzr;

import com.shade.decima.game.FileSystem;
import com.shade.decima.game.hrzr.rtti.HorizonTypeFactory;
import com.shade.decima.game.hrzr.rtti.HorizonZeroDawnRemastered;
import com.shade.decima.game.hrzr.storage.HorizonAssetId;
import com.shade.decima.game.hrzr.storage.HorizonAssetManager;
import com.shade.decima.game.hrzr.storage.PackFileManager;
import com.shade.decima.rtti.data.Ref;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Function;

import static com.shade.decima.game.hrzr.rtti.HorizonZeroDawnRemastered.ERenderPlatform;

public class DirectStorageReaderTest {
    private static final Logger log = LoggerFactory.getLogger(DirectStorageReaderTest.class);

    public static void main(String[] args) throws IOException {
        var fileSystem = new HorizonFileSystem(Path.of("E:/SteamLibrary/steamapps/common/Horizon Zero Dawn Remastered"));
        var typeFactory = new HorizonTypeFactory();

        log.info("Loading archives");
        try (var packFileManager = new PackFileManager(fileSystem)) {
            var assetManager = new HorizonAssetManager(packFileManager, typeFactory);
            var asset = assetManager.get(
                HorizonAssetId.ofPath("levels/game.core", "142071c5-7493-37d8-af34-361249017a88"),
                HorizonZeroDawnRemastered.Game.class
            );

            for (var levelGroup : deref(asset.general().levelGroups())) {
                log.debug("{}", levelGroup.general().name());
                for (var level : deref(levelGroup.general().levels())) {
                    log.debug("  {} - {}", level.general().name(), level.general().levelData());
                }
            }
        }
    }

    @NotNull
    private static <T> Iterable<T> deref(@NotNull Iterable<Ref<T>> iterable) {
        return map(iterable, Ref::get);
    }

    @NotNull
    private static <T, R> Iterable<R> map(@NotNull Iterable<T> iterable, @NotNull Function<T, R> mapper) {
        return () -> map(iterable.iterator(), mapper);
    }

    @NotNull
    private static <T, R> Iterator<R> map(@NotNull Iterator<T> iterator, @NotNull Function<T, R> mapper) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public R next() {
                return mapper.apply(iterator.next());
            }
        };
    }

    private record HorizonFileSystem(@NotNull Path source) implements FileSystem {
        @NotNull
        @Override
        public Path resolve(@NotNull String path) {
            String[] parts = path.split(":", 2);
            return switch (parts[0]) {
                case "source" -> source.resolve(parts[1]);
                case "cache" -> resolve("source:LocalCache" + ERenderPlatform.DX12).resolve(parts[1]);
                default -> throw new IllegalArgumentException("Unknown device path: " + path);
            };
        }
    }
}
