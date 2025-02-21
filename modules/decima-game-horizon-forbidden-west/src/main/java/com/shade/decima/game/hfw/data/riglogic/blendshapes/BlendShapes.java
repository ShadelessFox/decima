package com.shade.decima.game.hfw.data.riglogic.blendshapes;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public record BlendShapes(
    @NotNull short[] lods,
    @NotNull short[] inputIndices,
    @NotNull short[] outputIndices
) {
    @NotNull
    public static BlendShapes read(@NotNull BinaryReader reader) throws IOException {
        return new BlendShapes(
            reader.readShorts(reader.readInt()),
            reader.readShorts(reader.readInt()),
            reader.readShorts(reader.readInt())
        );
    }
}
