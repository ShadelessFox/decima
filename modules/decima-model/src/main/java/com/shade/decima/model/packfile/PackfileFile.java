package com.shade.decima.model.packfile;

import com.shade.decima.model.archive.Archive;
import com.shade.decima.model.archive.ArchiveFile;
import com.shade.util.NotNull;

import java.io.IOException;
import java.io.InputStream;

public class PackfileFile implements ArchiveFile {
    private final Packfile packfile;
    private final Packfile.FileEntry entry;

    public PackfileFile(@NotNull Packfile packfile, @NotNull Packfile.FileEntry entry) {
        this.packfile = packfile;
        this.entry = entry;
    }

    @NotNull
    @Override
    public String getName() {
        return "%#018x".formatted(entry.hash());
    }

    @NotNull
    @Override
    public String getPath() {
        return getName();
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
