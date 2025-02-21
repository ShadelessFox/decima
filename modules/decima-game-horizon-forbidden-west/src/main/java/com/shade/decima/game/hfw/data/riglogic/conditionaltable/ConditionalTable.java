package com.shade.decima.game.hfw.data.riglogic.conditionaltable;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

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
    public static ConditionalTable read(@NotNull BinaryReader reader) throws IOException {
        return new ConditionalTable(
            reader.readShorts(reader.readInt()),
            reader.readShorts(reader.readInt()),
            reader.readFloats(reader.readInt()),
            reader.readFloats(reader.readInt()),
            reader.readFloats(reader.readInt()),
            reader.readFloats(reader.readInt()),
            reader.readShort(),
            reader.readShort()
        );
    }
}
