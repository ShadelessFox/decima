package com.shade.decima.game.hfw.data.jolt.geometry;

import com.shade.decima.game.hfw.data.jolt.math.Vec3;
import com.shade.util.NotNull;

public record AABox(@NotNull Vec3 min, @NotNull Vec3 max) {
}
