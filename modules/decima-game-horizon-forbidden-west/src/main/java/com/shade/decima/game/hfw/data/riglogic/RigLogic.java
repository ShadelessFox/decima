package com.shade.decima.game.hfw.data.riglogic;

import com.shade.decima.game.hfw.data.riglogic.animatedmaps.AnimatedMaps;
import com.shade.decima.game.hfw.data.riglogic.blendshapes.BlendShapes;
import com.shade.decima.game.hfw.data.riglogic.controls.Controls;
import com.shade.decima.game.hfw.data.riglogic.joints.Joints;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public record RigLogic(
    @NotNull Configuration config,
    @NotNull RigMetrics metrics,
    @NotNull Controls controls,
    @NotNull Joints joints,
    @NotNull BlendShapes blendShapes,
    @NotNull AnimatedMaps animatedMaps
) {
    @NotNull
    public static RigLogic read(@NotNull BinaryReader reader) throws IOException {
        return new RigLogic(
            Configuration.read(reader),
            RigMetrics.read(reader),
            Controls.read(reader),
            Joints.read(reader),
            BlendShapes.read(reader),
            AnimatedMaps.read(reader)
        );
    }
}
