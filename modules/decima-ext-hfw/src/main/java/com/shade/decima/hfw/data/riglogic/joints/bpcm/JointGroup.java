package com.shade.decima.hfw.data.riglogic.joints.bpcm;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

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
    public static JointGroup read(@NotNull ByteBuffer buffer) {
        return new JointGroup(
            buffer.getInt(),
            buffer.getInt(),
            buffer.getInt(),
            buffer.getInt(),
            buffer.getInt(),
            buffer.getInt(),
            buffer.getInt(),
            buffer.getInt()
        );
    }
}
