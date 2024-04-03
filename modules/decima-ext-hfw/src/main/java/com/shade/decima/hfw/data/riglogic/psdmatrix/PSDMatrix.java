package com.shade.decima.hfw.data.riglogic.psdmatrix;

import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public record PSDMatrix(
    short distinctPSDs,
    @NotNull short[] rowIndices,
    @NotNull short[] columnIndices,
    @NotNull float[] values
) {
    @NotNull
    public static PSDMatrix read(@NotNull ByteBuffer buffer) {
        return new PSDMatrix(
            buffer.getShort(),
            BufferUtils.getShorts(buffer, buffer.getInt()),
            BufferUtils.getShorts(buffer, buffer.getInt()),
            BufferUtils.getFloats(buffer, buffer.getInt())
        );
    }
}
