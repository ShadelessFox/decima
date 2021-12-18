package com.shade.decima;

import com.shade.decima.archive.Archive;
import com.shade.decima.archive.ArchiveManager;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class Launcher {
    public static void main(String[] args) throws Exception {
        final Path root = Path.of("E:/SteamLibrary/steamapps/common/Death Stranding");
        final ArchiveManager manager = new ArchiveManager();
        final Compressor compressor = new Compressor(root.resolve("oo2core_7_win64.dll"));

        Files.walkFileTree(root.resolve("data"), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                manager.load(file);
                return FileVisitResult.CONTINUE;
            }
        });

        final Archive initial = manager.getArchive("initial");
        final Archive.FileEntry prefetch = manager.getFileEntry("prefetch/fullgame.prefetch");

        System.out.println(initial);
        System.out.println(prefetch);

        Files.write(Path.of("dump.bin"), initial.unpack(compressor, prefetch));
    }
}
