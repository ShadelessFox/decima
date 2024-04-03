package com.shade.decima.hfw.data.riglogic.joints.bpcm;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

record LODRegion(int size, int sizeAlignedToLastFullBlock, int sizeAlignedToSecondLastFullBlock) {
    @NotNull
    public static LODRegion read(@NotNull ByteBuffer buffer) {
        return new LODRegion(
            buffer.getInt(),
            buffer.getInt(),
            buffer.getInt()
        );
    }
}
