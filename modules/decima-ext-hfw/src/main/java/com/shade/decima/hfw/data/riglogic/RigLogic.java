package com.shade.decima.hfw.data.riglogic;

import com.shade.decima.hfw.data.riglogic.animatedmaps.AnimatedMaps;
import com.shade.decima.hfw.data.riglogic.blendshapes.BlendShapes;
import com.shade.decima.hfw.data.riglogic.controls.Controls;
import com.shade.decima.hfw.data.riglogic.joints.Joints;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public record RigLogic(
    @NotNull Configuration config,
    @NotNull RigMetrics metrics,
    @NotNull Controls controls,
    @NotNull Joints joints,
    @NotNull BlendShapes blendShapes,
    @NotNull AnimatedMaps animatedMaps
) {
    @NotNull
    public static RigLogic read(@NotNull ByteBuffer buffer) {
        return new RigLogic(
            Configuration.read(buffer),
            RigMetrics.read(buffer),
            Controls.read(buffer),
            Joints.read(buffer),
            BlendShapes.read(buffer),
            AnimatedMaps.read(buffer)
        );
    }
}
