package com.shade.decima.game.hfw.data.riglogic;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

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
    public static RigMetrics read(@NotNull BinaryReader reader) throws IOException {
        return new RigMetrics(
            reader.readShort(),
            reader.readShort(),
            reader.readShort(),
            reader.readShort(),
            reader.readShort(),
            reader.readShort(),
            reader.readShort()
        );
    }
}
