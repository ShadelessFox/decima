package com.shade.decima.game.hfw.data.riglogic.joints.bpcm;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.List;

public record JointStorage(
    @NotNull short[] values,
    @NotNull short[] inputIndices,
    @NotNull short[] outputIndices,
    @NotNull List<LODRegion> lodRegions,
    @NotNull List<JointGroup> jointGroups
) {
    @NotNull
    public static JointStorage read(@NotNull BinaryReader reader) throws IOException {
        return new JointStorage(
            reader.readShorts(reader.readInt()),
            reader.readShorts(reader.readInt()),
            reader.readShorts(reader.readInt()),
            reader.readObjects(reader.readInt(), LODRegion::read),
            reader.readObjects(reader.readInt(), JointGroup::read)
        );
    }
}
