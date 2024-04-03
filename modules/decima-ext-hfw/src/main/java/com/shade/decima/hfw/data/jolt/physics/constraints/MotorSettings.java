package com.shade.decima.hfw.data.jolt.physics.constraints;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class MotorSettings {
    private float frequency;
    private float damping;
    private float minForceLimit;
    private float maxForceLimit;
    private float minTorqueLimit;
    private float maxTorqueLimit;

    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        frequency = buffer.getFloat();
        damping = buffer.getFloat();
        minForceLimit = buffer.getFloat();
        maxForceLimit = buffer.getFloat();
        minTorqueLimit = buffer.getFloat();
        maxTorqueLimit = buffer.getFloat();
    }
}
