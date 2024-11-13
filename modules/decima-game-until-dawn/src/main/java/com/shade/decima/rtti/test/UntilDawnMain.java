package com.shade.decima.rtti.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class UntilDawnMain {
    private static final Logger log = LoggerFactory.getLogger(UntilDawnMain.class);

    public static void main(String[] args) throws IOException {
        var factory = new UntilDawnTypeFactory();
        var cache = Path.of("D:/PlayStation Games/Until Dawn/localcachepink");

        Files.walkFileTree(cache.resolve("lumps"), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String filename = file.getFileName().toString();
                if (!filename.endsWith(".core")) {
                    return FileVisitResult.CONTINUE;
                }
                if (filename.equals("assets_description.lightingsetups_lightingsetups.core")) {
                    log.info("Skipping {}", file);
                    return FileVisitResult.CONTINUE;
                }
                log.info("Reading {}", file);
                try (UntilDawnReader reader = new UntilDawnReader(CompressedBinaryReader.open(file), factory)) {
                    var objects = reader.read();
                    log.info("Read {} objects", objects.size());
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
