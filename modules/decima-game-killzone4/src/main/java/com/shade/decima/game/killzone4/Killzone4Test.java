package com.shade.decima.game.killzone4;

import com.shade.decima.game.killzone4.rtti.Killzone4TypeFactory;
import com.shade.decima.game.killzone4.rtti.Killzone4TypeReader;
import com.shade.util.io.BinaryReader;
import com.shade.util.io.CompressedBinaryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;
import java.util.stream.Stream;

public class Killzone4Test {
    private static final Logger log = LoggerFactory.getLogger(Killzone4Test.class);

    public static void main(String[] args) throws IOException {
        var lumps = Path.of("D:/PlayStation Games/Killzone Shadow Fall/localcachepink/lumps");
        var paths = new TreeMap<String, Path>();
        try (Stream<Path> stream = Files.list(lumps)) {
            stream
                .filter(p -> p.getFileName().toString().endsWith(".core"))
                .forEach(p -> paths.put(p.getFileName().toString(), p));
        }

        var factory = new Killzone4TypeFactory();
        for (Path path : paths.values()) {
            log.info("Reading {}", path);
            try (BinaryReader reader = CompressedBinaryReader.open(path)) {
                var objects = new Killzone4TypeReader().read(reader, factory);
                log.info("Read {} objects", objects.size());
            }
        }
    }
}
