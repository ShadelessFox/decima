package com.shade.decima.hfw.data.jolt.physics.constraints;

import com.shade.decima.hfw.data.jolt.JoltUtils;
import com.shade.util.NotNull;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class SliderConstraintSettings extends TwoBodyConstraintSettings {
    private Vector3f sliderAxis;
    private float limitsMin;
    private float limitsMax;
    private float maxFrictionForce;
    private final MotorSettings swingMotorSettings = new MotorSettings();

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);

        sliderAxis = JoltUtils.getAlignedVector3(buffer);
        limitsMin = buffer.getFloat();
        limitsMax = buffer.getFloat();
        maxFrictionForce = buffer.getFloat();
        swingMotorSettings.restoreBinaryState(buffer);
    }
}