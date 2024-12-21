package com.shade.decima.game.hfw.storage;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;
import com.shade.util.io.DirectStorageReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class StorageReadDevice implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(StorageReadDevice.class);

    private final Map<String, BinaryReader> files = new HashMap<>();
    private final PathResolver resolver;

    public StorageReadDevice(@NotNull PathResolver resolver) {
        this.resolver = resolver;
    }

    public void mount(@NotNull String file) throws IOException {
        if (files.containsKey(file)) {
            log.warn("File already mounted: {}", file);
            return;
        }

        Path path = resolver.resolve(file);
        if (Files.notExists(path)) {
            log.warn("File not found: {}", file);
            return;
        }


        BinaryReader reader;

        try {
            reader = DirectStorageReader.open(path);
        } catch (IOException e) {
            reader = BinaryReader.open(path);
        }

        files.put(file, reader);
        log.info("Mounting file: {}", file);
    }

    @NotNull
    public BinaryReader resolve(@NotNull String file) {
        BinaryReader reader = files.get(file);
        if (reader == null) {
            throw new IllegalArgumentException("Can't resolve file: " + file);
        }
        return reader;
    }

    @Override
    public void close() throws IOException {
        for (BinaryReader value : files.values()) {
            value.close();
        }
        files.clear();
    }
}
