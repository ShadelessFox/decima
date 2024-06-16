package com.shade.decima.hfw;

import com.shade.platform.model.ElementFactory;
import com.shade.platform.model.ServiceManager;
import com.shade.platform.model.app.Application;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.nio.file.Path;

public class TestApplication implements Application {
    private ServiceManager serviceManager;

    @Override
    public void start(@NotNull String[] args) {
        serviceManager = new ServiceManager(Path.of("samples/hfw/workspace.json"));
    }

    @Override
    public <T> T getService(@NotNull Class<T> cls) {
        return serviceManager.getService(cls);
    }

    @Nullable
    @Override
    public ElementFactory getElementFactory(@NotNull String id) {
        throw new NotImplementedException();
    }
}
