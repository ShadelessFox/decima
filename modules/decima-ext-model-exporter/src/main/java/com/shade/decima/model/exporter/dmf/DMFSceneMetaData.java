package com.shade.decima.model.exporter.dmf;

import com.shade.util.NotNull;

public record DMFSceneMetaData(@NotNull String generator, int version) {
}
