package com.shade.decima.hfw.data.riglogic.joints.bpcm;

import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public record JointStorage(
    @NotNull short[] values,
    @NotNull short[] inputIndices,
    @NotNull short[] outputIndices,
    @NotNull LODRegion[] lodRegions,
    @NotNull JointGroup[] jointGroups
) {
    @NotNull
    public static JointStorage read(@NotNull ByteBuffer buffer) {
        return new JointStorage(
            BufferUtils.getShorts(buffer, buffer.getInt()),
            BufferUtils.getShorts(buffer, buffer.getInt()),
            BufferUtils.getShorts(buffer, buffer.getInt()),
            BufferUtils.getObjects(buffer, buffer.getInt(), LODRegion[]::new, LODRegion::read),
            BufferUtils.getObjects(buffer, buffer.getInt(), JointGroup[]::new, JointGroup::read)
        );
    }
}
