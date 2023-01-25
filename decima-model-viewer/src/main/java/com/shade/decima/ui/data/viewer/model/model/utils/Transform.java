package com.shade.decima.ui.data.viewer.model.model.utils;

import com.shade.util.NotNull;

public record Transform(@NotNull Vector3 translation, @NotNull Quaternion rotation, @NotNull Vector3 scale) {
    public Transform(@NotNull double[] translation, @NotNull double[] rotation, @NotNull double[] scale) {
        this(new Vector3(translation), new Quaternion(rotation), new Vector3(scale));
    }
}
