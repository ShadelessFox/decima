package com.shade.decima.hfw.data.riglogic.controls;

import com.shade.decima.hfw.data.riglogic.conditionaltable.ConditionalTable;
import com.shade.decima.hfw.data.riglogic.psdmatrix.PSDMatrix;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public record Controls(
    @NotNull ConditionalTable guiToRawMapping,
    @NotNull PSDMatrix psds
) {
    @NotNull
    public static Controls read(@NotNull ByteBuffer buffer) {
        return new Controls(
            ConditionalTable.read(buffer),
            PSDMatrix.read(buffer)
        );
    }
}
