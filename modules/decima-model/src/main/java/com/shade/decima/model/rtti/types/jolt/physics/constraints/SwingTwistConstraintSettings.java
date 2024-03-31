package com.shade.decima.model.rtti.types.jolt.physics.constraints;

import com.shade.decima.model.rtti.types.jolt.JoltUtils;
import com.shade.util.NotNull;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class SwingTwistConstraintSettings extends TwoBodyConstraintSettings {
    Vector3f mPosition1;
    Vector3f mTwistAxis1;
    Vector3f mPlaneAxis1;
    Vector3f mPosition2;
    Vector3f mTwistAxis2;
    Vector3f mPlaneAxis2;
    float mNormalHalfConeAngle;
    float mPlaneHalfConeAngle;
    float mTwistMinAngle;
    float mTwistMaxAngle;
    float mMaxFrictionTorque;

    MotorSettings mSwingMotorSettings = new MotorSettings();
    MotorSettings mTwistMotorSettings = new MotorSettings();

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);

        mPosition1 = JoltUtils.getAlignedVector3(buffer);
        mTwistAxis1 = JoltUtils.getAlignedVector3(buffer);
        mPlaneAxis1 = JoltUtils.getAlignedVector3(buffer);
        mPosition2 = JoltUtils.getAlignedVector3(buffer);
        mTwistAxis2 = JoltUtils.getAlignedVector3(buffer);
        mPlaneAxis2 = JoltUtils.getAlignedVector3(buffer);
        mNormalHalfConeAngle = buffer.getFloat();
        mPlaneHalfConeAngle = buffer.getFloat();
        mTwistMinAngle = buffer.getFloat();
        mTwistMaxAngle = buffer.getFloat();
        mMaxFrictionTorque = buffer.getFloat();
        mSwingMotorSettings.restoreBinaryState(buffer);
        mTwistMotorSettings.restoreBinaryState(buffer);
    }
}
