package com.shade.decima.hfw.data.riglogic;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public record RigMetrics(
    short lodCount,
    short guiControlCount,
    short rawControlCount,
    short psdCount,
    short jointAttributeCount,
    short blendShapeCount,
    short animatedMapCount
) {
    @NotNull
    public static RigMetrics read(@NotNull ByteBuffer buffer) {
        return new RigMetrics(
            buffer.getShort(),
            buffer.getShort(),
            buffer.getShort(),
            buffer.getShort(),
            buffer.getShort(),
            buffer.getShort(),
            buffer.getShort()
        );
    }
}
