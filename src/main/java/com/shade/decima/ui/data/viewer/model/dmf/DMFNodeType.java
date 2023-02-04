package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.util.NotNull;

public enum DMFNodeType {
    NODE("Node"),
    MODEL_GROUP("ModelGroup"),
    LOD("LOD"),
    INSTANCE("Instance"),
    MODEL("Model");

    private final String name;

    DMFNodeType(@NotNull String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
