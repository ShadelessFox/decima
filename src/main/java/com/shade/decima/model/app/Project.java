package com.shade.decima.model.app;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.Compressor;
import com.shade.decima.model.util.Lazy;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.prefs.Preferences;

public class Project implements Closeable {
    private final String id;
    private final String name;
    private final GameType type;

    private final Path executablePath;
    private final Path archivesRootPath;
    private final Lazy<RTTITypeRegistry> typeRegistry;
    private final Lazy<PackfileManager> packfileManager;
    private final Lazy<Compressor> compressor;

    public Project(@NotNull String id, @NotNull String name, @NotNull GameType type, @NotNull Path executablePath, @NotNull Path archivesRootPath, @NotNull Path rttiExternalTypeInfoPath, @Nullable Path packfileInfoPath, @NotNull Path compressorPath) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.executablePath = executablePath;
        this.archivesRootPath = archivesRootPath;

        this.typeRegistry = Lazy.of(() -> new RTTITypeRegistry(rttiExternalTypeInfoPath, type));
        this.compressor = Lazy.of(() -> new Compressor(compressorPath, Compressor.Level.NORMAL));
        this.packfileManager = Lazy.of(() -> new PackfileManager(compressor.get(), packfileInfoPath));
    }

    public Project(@NotNull String id, @NotNull Preferences node) {
        this(
            id,
            node.get("game_name", null),
            GameType.valueOf(node.get("game_type", null)),
            Path.of(node.get("game_executable_path", null)),
            Path.of(node.get("game_archive_root_path", null)),
            Path.of(node.get("game_rtti_meta_path", null)),
            Optional.ofNullable(node.get("game_archive_meta_path", null)).map(Path::of).orElse(null),
            Path.of(node.get("game_compressor_path", null))
        );
    }

    public void loadArchives() throws IOException {
        final PackfileManager manager = packfileManager.get();

        Files.walkFileTree(archivesRootPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(".bin")) {
                    manager.mount(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public Path getExecutablePath() {
        return executablePath;
    }

    @NotNull
    public Path getArchivesRootPath() {
        return archivesRootPath;
    }

    @NotNull
    public RTTITypeRegistry getTypeRegistry() {
        return typeRegistry.get();
    }

    @NotNull
    public PackfileManager getPackfileManager() {
        return packfileManager.get();
    }

    @NotNull
    public Compressor getCompressor() {
        return compressor.get();
    }

    @NotNull
    public GameType getType() {
        return type;
    }

    @Override
    public void close() throws IOException {
        packfileManager.clear(PackfileManager::close);
        compressor.clear(Compressor::close);
        typeRegistry.clear();
    }
}
