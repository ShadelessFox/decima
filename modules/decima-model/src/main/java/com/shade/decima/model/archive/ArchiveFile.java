package com.shade.decima.model.archive;

import com.shade.util.NotNull;

import java.io.IOException;
import java.io.InputStream;

public interface ArchiveFile {
    long getIdentifier();

    @NotNull
    Archive getArchive();

    long getLength();

    @NotNull
    InputStream newInputStream() throws IOException;

    @NotNull
    default byte[] readAllBytes() throws IOException {
        try (InputStream in = newInputStream()) {
            return in.readAllBytes();
        }
    }
}
