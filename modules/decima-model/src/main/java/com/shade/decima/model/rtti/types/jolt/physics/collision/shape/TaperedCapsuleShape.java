package com.shade.decima.model.rtti.types.jolt.physics.collision.shape;

import com.shade.decima.model.rtti.types.jolt.JoltUtils;
import com.shade.util.NotNull;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class TaperedCapsuleShape extends ConvexShape {
    private Vector3f mCenterOfMass;
    private float mTopRadius;
    private float mBottomRadius;
    private float mTopCenter;
    private float mBottomCenter;
    private float mConvexRadius;
    private float mSinAlpha;
    private float mTanAlpha;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);

        mCenterOfMass = JoltUtils.getAlignedVector3(buffer);
        mTopRadius = buffer.getFloat();
        mBottomRadius = buffer.getFloat();
        mTopCenter = buffer.getFloat();
        mBottomCenter = buffer.getFloat();
        mConvexRadius = buffer.getFloat();
        mSinAlpha = buffer.getFloat();
        mTanAlpha = buffer.getFloat();
    }
}
