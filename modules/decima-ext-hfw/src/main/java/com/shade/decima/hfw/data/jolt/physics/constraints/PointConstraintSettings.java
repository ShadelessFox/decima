package com.shade.decima.hfw.data.jolt.physics.constraints;

import com.shade.decima.hfw.data.jolt.JoltUtils;
import com.shade.util.NotNull;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class PointConstraintSettings extends TwoBodyConstraintSettings {
    private Vector3f commonPoint;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);

        commonPoint = JoltUtils.getAlignedVector3(buffer);
    }
}
