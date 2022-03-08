package com.shade.decima.model.app;

import com.shade.decima.model.archive.ArchiveManager;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.Compressor;
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
    private final Path executablePath;
    private final Path archivesRootPath;
    private final RTTITypeRegistry typeRegistry;
    private final ArchiveManager archiveManager;
    private final Compressor compressor;
    private final GameType gameType;

    public Project(@NotNull String id, @NotNull Path executablePath, @NotNull Path archivesRootPath, @NotNull Path rttiExternalTypeInfoPath, @Nullable Path archiveInfoPath, @NotNull Path compressorPath, @NotNull GameType gameType) {
        this.id = id;
        this.executablePath = executablePath;
        this.archivesRootPath = archivesRootPath;

        this.typeRegistry = new RTTITypeRegistry(rttiExternalTypeInfoPath, gameType);
        this.archiveManager = new ArchiveManager(typeRegistry, archiveInfoPath);
        this.compressor = new Compressor(compressorPath, Compressor.Level.NORMAL);
        this.gameType = gameType;
    }

    public void loadArchives() throws IOException {
        Files.walkFileTree(archivesRootPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(".bin")) {
                    archiveManager.load(file);
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
    public Path getExecutablePath() {
        return executablePath;
    }

    @NotNull
    public Path getArchivesRootPath() {
        return archivesRootPath;
    }

    @NotNull
    public RTTITypeRegistry getTypeRegistry() {
        return typeRegistry;
    }

    @NotNull
    public ArchiveManager getArchiveManager() {
        return archiveManager;
    }

    @NotNull
    public Compressor getCompressor() {
        return compressor;
    }

    @NotNull
    public GameType getGameType() {
        return gameType;
    }

    @Override
    public void close() throws IOException {
        archiveManager.close();
    }
}
