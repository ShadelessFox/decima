package com.shade.decima.game.hfw.data.riglogic.joints;

import com.shade.decima.game.hfw.data.riglogic.joints.bpcm.JointStorage;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public record JointsEvaluator(
    @NotNull JointStorage storage
) {
    @NotNull
    public static JointsEvaluator read(@NotNull BinaryReader reader) throws IOException {
        return new JointsEvaluator(JointStorage.read(reader));
    }
}
