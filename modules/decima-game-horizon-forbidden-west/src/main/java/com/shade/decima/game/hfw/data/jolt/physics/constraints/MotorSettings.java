package com.shade.decima.game.hfw.data.jolt.physics.constraints;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class MotorSettings {
    public float frequency;
    public float damping;
    public float minForceLimit;
    public float maxForceLimit;
    public float minTorqueLimit;
    public float maxTorqueLimit;

    public void restoreBinaryState(@NotNull BinaryReader reader) throws IOException {
        frequency = reader.readFloat();
        damping = reader.readFloat();
        minForceLimit = reader.readFloat();
        maxForceLimit = reader.readFloat();
        minTorqueLimit = reader.readFloat();
        maxTorqueLimit = reader.readFloat();
    }
}
