package com.shade.decima.game.hfw.data.jolt.physics.constraints;

import com.shade.decima.game.hfw.data.jolt.JoltUtils;
import com.shade.decima.game.hfw.data.jolt.math.Vec3;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class PointConstraintSettings extends TwoBodyConstraintSettings {
    public Vec3 commonPoint;

    @Override
    public void restoreBinaryState(@NotNull BinaryReader reader) throws IOException {
        super.restoreBinaryState(reader);

        commonPoint = JoltUtils.readAlignedVector3(reader);
    }
}
