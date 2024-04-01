package com.shade.decima.model.rtti.types.jolt.physics.constraints;

import com.shade.decima.model.rtti.types.jolt.JoltUtils;
import com.shade.util.NotNull;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class HingeConstraintSettings extends TwoBodyConstraintSettings {

    Vector3f point1 = new Vector3f(0.0f);
    Vector3f hingeAxis1 = new Vector3f(0.0f, 1.0f, 0.0f);
    Vector3f normalAxis1 = new Vector3f(1.0f, 0.0f, 0.0f);
    Vector3f point2 = new Vector3f(0.0f);
    Vector3f hingeAxis2 = new Vector3f(0.0f, 1.0f, 0.0f);
    Vector3f normalAxis2 = new Vector3f(1.0f, 0.0f, 0.0f);
    float limitsMin = (float) -Math.PI;
    float limitsMax = (float) Math.PI;
    float maxFrictionForce = 0.0f;
    MotorSettings motorSettings = new MotorSettings();

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);
        point1 = JoltUtils.getAlignedVector3(buffer);
        hingeAxis1 = JoltUtils.getAlignedVector3(buffer);
        normalAxis1 = JoltUtils.getAlignedVector3(buffer);
        point2 = JoltUtils.getAlignedVector3(buffer);
        hingeAxis2 = JoltUtils.getAlignedVector3(buffer);
        normalAxis2 = JoltUtils.getAlignedVector3(buffer);
        limitsMin = buffer.getFloat();
        limitsMax = buffer.getFloat();
        maxFrictionForce = buffer.getFloat();
        motorSettings.restoreBinaryState(buffer);

    }
}
