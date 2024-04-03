package com.shade.decima.hfw.data.riglogic.joints;

import com.shade.decima.hfw.data.riglogic.joints.bpcm.JointStorage;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public record JointsEvaluator(
    @NotNull JointStorage storage
) {
    @NotNull
    public static JointsEvaluator read(@NotNull ByteBuffer buffer) {
        return new JointsEvaluator(JointStorage.read(buffer));
    }
}
