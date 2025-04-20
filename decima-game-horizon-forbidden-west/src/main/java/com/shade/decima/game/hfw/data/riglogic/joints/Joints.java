package com.shade.decima.game.hfw.data.riglogic.joints;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public record Joints(
    @NotNull JointsEvaluator evaluator,
    @NotNull float[] neutralValues,
    @NotNull short[][] variableAttributeIndices,
    short jointGroupCount
) {
    public static Joints read(@NotNull BinaryReader reader) throws IOException {
        return new Joints(
            JointsEvaluator.read(reader),
            reader.readFloats(reader.readInt()),
            reader.readObjects(reader.readInt(), r -> r.readShorts(r.readInt()), short[][]::new),
            reader.readShort()
        );
    }
}
