package com.shade.decima.hfw.data.riglogic.conditionaltable;

import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public record ConditionalTable(
    @NotNull short[] inputIndices,
    @NotNull short[] outputIndices,
    @NotNull float[] fromValues,
    @NotNull float[] toValues,
    @NotNull float[] slopeValues,
    @NotNull float[] cutValues,
    short inputCount,
    short outputCount
) {
    @NotNull
    public static ConditionalTable read(@NotNull ByteBuffer buffer) {
        return new ConditionalTable(
            BufferUtils.getShorts(buffer, buffer.getInt()),
            BufferUtils.getShorts(buffer, buffer.getInt()),
            BufferUtils.getFloats(buffer, buffer.getInt()),
            BufferUtils.getFloats(buffer, buffer.getInt()),
            BufferUtils.getFloats(buffer, buffer.getInt()),
            BufferUtils.getFloats(buffer, buffer.getInt()),
            buffer.getShort(),
            buffer.getShort()
        );
    }
}
