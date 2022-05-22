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

public class Project implements Closeable {
    private final String id;
    private final String name;
    private final Path executablePath;
    private final Path archivesRootPath;
    private final Lazy<RTTITypeRegistry> typeRegistry;
    private final Lazy<PackfileManager> packfileManager;
    private final Lazy<Compressor> compressor;
    private final GameType gameType;

    public Project(@NotNull String id, @NotNull String name, @NotNull Path executablePath, @NotNull Path archivesRootPath, @NotNull Path rttiExternalTypeInfoPath, @Nullable Path packfileInfoPath, @NotNull Path compressorPath, @NotNull GameType gameType) {
        this.id = id;
        this.name = name;
        this.executablePath = executablePath;
        this.archivesRootPath = archivesRootPath;

        this.typeRegistry = Lazy.of(() -> new RTTITypeRegistry(rttiExternalTypeInfoPath, gameType));
        this.compressor = Lazy.of(() -> new Compressor(compressorPath, Compressor.Level.NORMAL));
        this.packfileManager = Lazy.of(() -> new PackfileManager(compressor.get(), packfileInfoPath));
        this.gameType = gameType;
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
    public GameType getGameType() {
        return gameType;
    }

    @Override
    public void close() throws IOException {
        packfileManager.ifLoaded(PackfileManager::close);
    }
}
