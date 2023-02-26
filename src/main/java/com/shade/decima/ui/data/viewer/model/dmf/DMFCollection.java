package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

public record DMFCollection(@NotNull String name, boolean enabled, @Nullable Integer parent) {
}
