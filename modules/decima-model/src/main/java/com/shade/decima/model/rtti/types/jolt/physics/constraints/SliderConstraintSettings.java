package com.shade.decima.model.rtti.types.jolt.physics.constraints;

import com.shade.decima.model.rtti.types.jolt.JoltUtils;
import com.shade.util.NotNull;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class SliderConstraintSettings extends TwoBodyConstraintSettings {

    Vector3f sliderAxis = new Vector3f(0, 1, 0);
    float limitsMin = -Float.MAX_VALUE;
    float limitsMax = Float.MAX_VALUE;
    float maxFrictionForce = 0.0f;
    MotorSettings swingMotorSettings = new MotorSettings();

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
