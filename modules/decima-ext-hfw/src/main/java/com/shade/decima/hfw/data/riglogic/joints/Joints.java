package com.shade.decima.hfw.data.riglogic.joints;

import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public record Joints(
    @NotNull JointsEvaluator evaluator,
    @NotNull float[] neutralValues,
    @NotNull short[][] variableAttributeIndices,
    short jointGroupCount
) {
    public static Joints read(@NotNull ByteBuffer buffer) {
        return new Joints(
            JointsEvaluator.read(buffer),
            BufferUtils.getFloats(buffer, buffer.getInt()),
            BufferUtils.getObjects(buffer, buffer.getInt(), short[][]::new, buf -> BufferUtils.getShorts(buf, buf.getInt())),
            buffer.getShort()
        );
    }
}
