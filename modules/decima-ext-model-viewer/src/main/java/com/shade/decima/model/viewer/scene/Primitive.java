package com.shade.decima.model.viewer.scene;

import com.shade.gl.Attribute;
import com.shade.util.NotNull;

import java.util.Map;

/**
 * Represents bare primitive that can be rendered.
 */
public record Primitive(@NotNull Map<Attribute.Semantic, Accessor> attributes, @NotNull Accessor indices, int hash) {
}
