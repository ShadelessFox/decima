package com.shade.decima.model.rtti;

import com.shade.decima.model.archive.ArchiveFile;
import com.shade.util.NotNull;

import java.io.IOException;
import java.io.InputStream;

public interface RTTICoreFileReader {
    @NotNull
    RTTICoreFile read(@NotNull InputStream is, boolean lenient) throws IOException;

    @NotNull
    byte[] write(@NotNull RTTICoreFile file);

    @NotNull
    default RTTICoreFile read(@NotNull ArchiveFile file, boolean lenient) throws IOException {
        try (InputStream is = file.newInputStream()) {
            return read(is, lenient);
        }
    }
}
