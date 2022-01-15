package com.shade.decima;

import com.shade.decima.archive.ArchiveManager;
import com.shade.decima.util.Compressor;
import com.shade.decima.util.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class Project implements Closeable {
    private final Path executablePath;
    private final ArchiveManager archiveManager;
    private final Compressor compressor;

    public Project(@NotNull Path executablePath) {
        this.executablePath = executablePath;
        this.archiveManager = new ArchiveManager();
        this.compressor = new Compressor(executablePath.getParent().resolve("oo2core_7_win64.dll"));
    }

    public void loadArchives() throws IOException {
        Files.walkFileTree(executablePath.getParent(), new SimpleFileVisitor<>() {
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
    public Path getExecutablePath() {
        return executablePath;
    }

    @NotNull
    public ArchiveManager getArchiveManager() {
        return archiveManager;
    }

    @NotNull
    public Compressor getCompressor() {
        return compressor;
    }

    @Override
    public void close() throws IOException {
        archiveManager.close();
    }
}
