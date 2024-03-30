package com.shade.decima.model.rtti.types.jolt.geometry;

import com.shade.util.NotNull;
import org.joml.Vector3f;

public record Plane(@NotNull Vector3f normal, float distance) {
}
