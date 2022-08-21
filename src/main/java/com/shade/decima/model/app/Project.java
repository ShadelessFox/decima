package com.shade.decima.model.app;

import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.Compressor;
import com.shade.util.NotNull;

import java.io.Closeable;
import java.io.IOException;

public class Project implements Closeable {
    private final ProjectContainer container;
    private final RTTITypeRegistry typeRegistry;
    private final PackfileManager packfileManager;
    private final Compressor compressor;
    private final ProjectPersister persister;

    public Project(@NotNull ProjectContainer container) {
        this.container = container;
        this.typeRegistry = new RTTITypeRegistry(container);
        this.compressor = new Compressor(container.getCompressorPath());
        this.packfileManager = new PackfileManager(compressor, container.getPackfileMetadataPath());
        this.persister = new ProjectPersister();
    }

    public void mountDefaults() throws IOException {
        packfileManager.mountDefaults(container.getPackfilesPath());
    }

    @NotNull
    public ProjectContainer getContainer() {
        return container;
    }

    @NotNull
    public RTTITypeRegistry getTypeRegistry() {
        return typeRegistry;
    }

    @NotNull
    public PackfileManager getPackfileManager() {
        return packfileManager;
    }

    @NotNull
    public Compressor getCompressor() {
        return compressor;
    }

    @NotNull
    public ProjectPersister getPersister() {
        return persister;
    }

    @Override
    public void close() throws IOException {
        packfileManager.close();
        compressor.close();
    }
}
