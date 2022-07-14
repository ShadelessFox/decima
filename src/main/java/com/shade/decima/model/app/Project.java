package com.shade.decima.model.app;

import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.Compressor;
import com.shade.decima.model.util.NotNull;

import java.io.Closeable;
import java.io.IOException;

public class Project implements Closeable {
    private final ProjectContainer container;
    private final RTTITypeRegistry typeRegistry;
    private final PackfileManager packfileManager;
    private final Compressor compressor;

    public Project(@NotNull ProjectContainer container) {
        this.container = container;
        this.typeRegistry = new RTTITypeRegistry(container.getTypeMetadataPath(), container.getType());
        this.compressor = new Compressor(container.getCompressorPath(), Compressor.Level.NORMAL);
        this.packfileManager = new PackfileManager(compressor, container.getPackfileMetadataPath());
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

    @Override
    public void close() throws IOException {
        packfileManager.close();
        compressor.close();
    }
}
