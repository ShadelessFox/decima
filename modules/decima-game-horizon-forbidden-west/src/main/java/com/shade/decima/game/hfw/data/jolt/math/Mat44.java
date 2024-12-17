package com.shade.decima.game.hfw.data.jolt.math;

import com.shade.util.NotNull;

public record Mat44(@NotNull Vec4 col0, @NotNull Vec4 col1, @NotNull Vec4 col2, @NotNull Vec4 col3) {
}
