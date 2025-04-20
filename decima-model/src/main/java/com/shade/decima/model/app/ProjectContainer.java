package com.shade.decima.model.app;

import com.shade.decima.model.base.GameType;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;

public class ProjectContainer {
    private final UUID id;
    private String name;
    private GameType type;

    private Path executablePath;
    private Path packfilesPath;
    private Path compressorPath;

    public ProjectContainer(
        @NotNull UUID id,
        @NotNull String name,
        @NotNull GameType type,
        @NotNull Path executablePath,
        @NotNull Path packfilesPath,
        @NotNull Path compressorPath
    ) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.executablePath = executablePath;
        this.packfilesPath = packfilesPath;
        this.compressorPath = compressorPath;
    }

    public ProjectContainer() {
        this.id = UUID.randomUUID();
    }

    @NotNull
    public UUID getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public GameType getType() {
        return type;
    }

    public void setType(@NotNull GameType type) {
        this.type = type;
    }

    @NotNull
    public Path getExecutablePath() {
        return executablePath;
    }

    public void setExecutablePath(@NotNull Path executablePath) {
        this.executablePath = executablePath;
    }

    @NotNull
    public Path getCompressorPath() {
        return compressorPath;
    }

    public void setCompressorPath(@NotNull Path compressorPath) {
        this.compressorPath = compressorPath;
    }

    @NotNull
    public Path getPackfilesPath() {
        return packfilesPath;
    }

    public void setPackfilesPath(@NotNull Path packfilesPath) {
        this.packfilesPath = packfilesPath;
    }

    @NotNull
    public BufferedReader getTypeMetadata() throws IOException {
        final String path = switch (type) {
            case DS -> "/metadata/ds_types.json.gz";
            case DSDC -> "/metadata/dsdc_types.json.gz";
            case HZD -> "/metadata/hzd_types.json.gz";
        };
        final InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            throw new IllegalStateException("Internal error: failed to locate file containing type metadata for " + type);
        }
        return IOUtils.newCompressedReader(is);
    }

    @NotNull
    public BufferedReader getFilePaths() throws IOException {
        final String path = switch (type) {
            case DS -> "/metadata/ds_paths.txt.gz";
            case DSDC -> "/metadata/dsdc_paths.txt.gz";
            case HZD -> "/metadata/hzd_paths.txt.gz";
        };
        final InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            throw new IllegalStateException("Internal error: failed to locate file containing file paths for " + type);
        }
        return IOUtils.newCompressedReader(is);
    }

    @Override
    public String toString() {
        return getName();
    }
}
