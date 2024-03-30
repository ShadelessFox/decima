package com.shade.decima.model.rtti.types.jolt.geometry;

import com.shade.util.NotNull;
import org.joml.Vector3f;

public record AABox(@NotNull Vector3f min, @NotNull Vector3f max) {
}
