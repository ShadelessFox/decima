package com.shade.decima.hfw.archive;

import com.shade.decima.hfw.HFWTest;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class StorageReadDevice implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(StorageReadDevice.class);

    private final Map<String, SeekableByteChannel> files = new HashMap<>();
    private final ProjectContainer project;

    public StorageReadDevice(@NotNull ProjectContainer project) {
        this.project = project;
    }

    public void mount(@NotNull String file) throws IOException {
        if (files.containsKey(file)) {
            log.warn("File already mounted: {}", file);
            return;
        }

        final Path path = HFWTest.expandPath(project, file);
        if (Files.notExists(path)) {
            log.warn("File not found: {}", file);
            return;
        }

        files.put(file, StorageChannel.newChannel(path));
        log.info("Mounted file: {}", file);
    }

    @NotNull
    public SeekableByteChannel resolve(@NotNull String file) {
        final SeekableByteChannel channel = files.get(file);
        if (channel == null) {
            throw new IllegalArgumentException("Can't resolve file: " + file);
        }
        return channel;
    }

    @Override
    public void close() throws IOException {
        for (Iterator<SeekableByteChannel> it = files.values().iterator(); it.hasNext(); ) {
            it.next().close();
            it.remove();
        }
    }
}
