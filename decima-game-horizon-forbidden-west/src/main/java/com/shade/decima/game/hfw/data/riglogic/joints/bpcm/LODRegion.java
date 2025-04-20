package com.shade.decima.game.hfw.data.riglogic.joints.bpcm;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

record LODRegion(int size, int sizeAlignedToLastFullBlock, int sizeAlignedToSecondLastFullBlock) {
    @NotNull
    public static LODRegion read(@NotNull BinaryReader reader) throws IOException {
        return new LODRegion(
            reader.readInt(),
            reader.readInt(),
            reader.readInt()
        );
    }
}
