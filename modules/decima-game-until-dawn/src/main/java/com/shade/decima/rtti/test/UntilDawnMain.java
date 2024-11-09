package com.shade.decima.rtti.test;

import com.shade.util.io.BinaryReader;
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
        Path cache = Path.of("D:/PlayStation Games/Until Dawn/localcachepink");
        Path lump = cache.resolve("lumps/assets_description.application_concreteasset.core");

        try (BinaryReader reader = CompressedBinaryReader.open(lump)) {
            Files.write(Path.of("samples/until_dawn/assets_description.application_concreteasset.core"), reader.readBytes(Math.toIntExact(reader.size())));
        }

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
                try (UntilDawnReader reader = new UntilDawnReader(CompressedBinaryReader.open(file))) {
                    var objects = reader.read();
                    log.info("Read {} objects", objects.size());
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
