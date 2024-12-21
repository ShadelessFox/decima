package com.shade.decima.game.hfw.data.riglogic.animatedmaps;

import com.shade.decima.game.hfw.data.riglogic.conditionaltable.ConditionalTable;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public record AnimatedMaps(
    @NotNull short[] lods,
    @NotNull ConditionalTable conditionals
) {
    @NotNull
    public static AnimatedMaps read(@NotNull BinaryReader reader) throws IOException {
        return new AnimatedMaps(
            reader.readShorts(reader.readInt()),
            ConditionalTable.read(reader)
        );
    }
}
