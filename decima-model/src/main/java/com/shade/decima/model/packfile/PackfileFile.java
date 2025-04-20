package com.shade.decima.model.packfile;

import com.shade.decima.model.archive.Archive;
import com.shade.decima.model.archive.ArchiveFile;
import com.shade.util.NotNull;

import java.io.IOException;
import java.io.InputStream;

public record PackfileFile(@NotNull Packfile packfile, @NotNull Packfile.FileEntry entry) implements ArchiveFile {
    @Override
    public long getIdentifier() {
        return entry.hash();
    }

    @NotNull
    @Override
    public Archive getArchive() {
        return packfile;
    }

    @Override
    public long getLength() {
        return entry.span().size();
    }

    @NotNull
    @Override
    public InputStream newInputStream() throws IOException {
        return packfile.newInputStream(entry.hash());
    }
}
