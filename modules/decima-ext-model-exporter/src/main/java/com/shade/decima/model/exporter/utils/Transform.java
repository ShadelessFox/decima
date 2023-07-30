package com.shade.decima.model.exporter.utils;

import com.shade.util.NotNull;

public record Transform(@NotNull Vector3 translation, @NotNull Quaternion rotation, @NotNull Vector3 scale) {
}
