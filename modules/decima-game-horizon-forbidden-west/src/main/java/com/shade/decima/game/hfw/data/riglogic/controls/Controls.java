package com.shade.decima.game.hfw.data.riglogic.controls;

import com.shade.decima.game.hfw.data.riglogic.conditionaltable.ConditionalTable;
import com.shade.decima.game.hfw.data.riglogic.psdmatrix.PSDMatrix;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public record Controls(
    @NotNull ConditionalTable guiToRawMapping,
    @NotNull PSDMatrix psds
) {
    @NotNull
    public static Controls read(@NotNull BinaryReader reader) throws IOException {
        return new Controls(
            ConditionalTable.read(reader),
            PSDMatrix.read(reader)
        );
    }
}
