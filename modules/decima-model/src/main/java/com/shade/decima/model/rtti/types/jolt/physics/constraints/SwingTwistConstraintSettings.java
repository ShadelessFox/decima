package com.shade.decima.model.rtti.types.jolt.physics.constraints;

import com.shade.decima.model.rtti.types.jolt.JoltUtils;
import com.shade.util.NotNull;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class SwingTwistConstraintSettings extends TwoBodyConstraintSettings {
    private Vector3f position1;
    private Vector3f twistAxis1;
    private Vector3f planeAxis1;
    private Vector3f position2;
    private Vector3f twistAxis2;
    private Vector3f planeAxis2;
    private float normalHalfConeAngle;
    private float planeHalfConeAngle;
    private float twistMinAngle;
    private float twistMaxAngle;
    private float maxFrictionTorque;
    private final MotorSettings swingMotorSettings = new MotorSettings();
    private final MotorSettings twistMotorSettings = new MotorSettings();

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);

        position1 = JoltUtils.getAlignedVector3(buffer);
        twistAxis1 = JoltUtils.getAlignedVector3(buffer);
        planeAxis1 = JoltUtils.getAlignedVector3(buffer);
        position2 = JoltUtils.getAlignedVector3(buffer);
        twistAxis2 = JoltUtils.getAlignedVector3(buffer);
        planeAxis2 = JoltUtils.getAlignedVector3(buffer);
        normalHalfConeAngle = buffer.getFloat();
        planeHalfConeAngle = buffer.getFloat();
        twistMinAngle = buffer.getFloat();
        twistMaxAngle = buffer.getFloat();
        maxFrictionTorque = buffer.getFloat();
        swingMotorSettings.restoreBinaryState(buffer);
        twistMotorSettings.restoreBinaryState(buffer);
    }
}
