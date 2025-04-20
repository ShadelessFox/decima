package com.shade.decima.game.hfw.data.riglogic.joints.bpcm;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

record JointGroup(
    int valuesOffset,
    int inputIndicesOffset,
    int outputIndicesOffset,
    int lodsOffset,
    int valuesSize,
    int inputIndicesSize,
    int inputIndicesSizeAlignedTo4,
    int inputIndicesSizeAlignedTo8
) {
    @NotNull
    public static JointGroup read(@NotNull BinaryReader reader) throws IOException {
        return new JointGroup(
            reader.readInt(),
            reader.readInt(),
            reader.readInt(),
            reader.readInt(),
            reader.readInt(),
            reader.readInt(),
            reader.readInt(),
            reader.readInt()
        );
    }
}
