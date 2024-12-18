package com.shade.decima.game.hfw.data.riglogic.psdmatrix;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public record PSDMatrix(
    short distinctPSDs,
    @NotNull short[] rowIndices,
    @NotNull short[] columnIndices,
    @NotNull float[] values
) {
    @NotNull
    public static PSDMatrix read(@NotNull BinaryReader reader) throws IOException {
        return new PSDMatrix(
            reader.readShort(),
            reader.readShorts(reader.readInt()),
            reader.readShorts(reader.readInt()),
            reader.readFloats(reader.readInt())
        );
    }
}
