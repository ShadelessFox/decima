package com.shade.decima.hfw.data.riglogic.animatedmaps;

import com.shade.decima.hfw.data.riglogic.conditionaltable.ConditionalTable;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public record AnimatedMaps(
    @NotNull short[] lods,
    @NotNull ConditionalTable conditionals
) {
    @NotNull
    public static AnimatedMaps read(@NotNull ByteBuffer buffer) {
        return new AnimatedMaps(
            BufferUtils.getShorts(buffer, buffer.getInt()),
            ConditionalTable.read(buffer)
        );
    }
}
