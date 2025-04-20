package com.shade.platform.model;

import com.shade.util.NotNull;

import java.util.Map;

public interface SaveableElement {
    void saveState(@NotNull Map<String, Object> state);

    @NotNull
    String getFactoryId();
}
