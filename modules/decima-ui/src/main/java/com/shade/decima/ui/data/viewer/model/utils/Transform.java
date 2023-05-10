package com.shade.decima.ui.data.viewer.model.utils;

import com.shade.util.NotNull;

public record Transform(@NotNull Vector3 translation, @NotNull Quaternion rotation, @NotNull Vector3 scale) {
}
