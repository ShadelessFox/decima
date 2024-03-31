package com.shade.decima.model.rtti.types.jolt.physics.constraints;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class MotorSettings {
    private float frequency = 2.0f;
    private float damping = 1.0f;
    private float minForceLimit = -Float.MAX_VALUE;
    private float maxForceLimit = Float.MAX_VALUE;
    private float minTorqueLimit = -Float.MAX_VALUE;
    private float maxTorqueLimit = Float.MAX_VALUE;

    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        frequency = buffer.getFloat();
        damping = buffer.getFloat();
        minForceLimit = buffer.getFloat();
        maxForceLimit = buffer.getFloat();
        minTorqueLimit = buffer.getFloat();
        maxTorqueLimit = buffer.getFloat();
    }
}
