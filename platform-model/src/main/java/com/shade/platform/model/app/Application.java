package com.shade.platform.model.app;

import com.shade.platform.model.ElementFactory;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.time.LocalDateTime;

public interface Application {
    void start(@NotNull String[] args);

    <T> T getService(@NotNull Class<T> cls);

    @Nullable
    ElementFactory getElementFactory(@NotNull String id);

    @NotNull
    String getTitle();

    @NotNull
    String getVersion();

    @NotNull
    String getBuildNumber();

    @NotNull
    LocalDateTime getBuildTime();
}
