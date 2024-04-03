package com.shade.decima.hfw.data.riglogic.blendshapes;

import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public record BlendShapes(
    @NotNull short[] lods,
    @NotNull short[] inputIndices,
    @NotNull short[] outputIndices
) {
    @NotNull
    public static BlendShapes read(@NotNull ByteBuffer buffer) {
        return new BlendShapes(
            BufferUtils.getShorts(buffer, buffer.getInt()),
            BufferUtils.getShorts(buffer, buffer.getInt()),
            BufferUtils.getShorts(buffer, buffer.getInt())
        );
    }
}
