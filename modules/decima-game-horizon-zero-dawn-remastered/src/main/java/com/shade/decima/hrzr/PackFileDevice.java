package com.shade.decima.hrzr;

import com.shade.decima.rtti.PathResolver;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PackFileDevice implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(PackFileDevice.class);

    private final Map<String, BinaryReader> files = new HashMap<>();
    private final PathResolver resolver;

    public PackFileDevice(@NotNull PathResolver resolver) {
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

        files.put(file, DirectStorageReader.open(path));
        log.info("Mounted file: {}", file);
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
