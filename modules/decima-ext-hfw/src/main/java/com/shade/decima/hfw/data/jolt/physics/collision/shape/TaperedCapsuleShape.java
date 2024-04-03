package com.shade.decima.hfw.data.jolt.physics.collision.shape;

import com.shade.decima.hfw.data.jolt.JoltUtils;
import com.shade.util.NotNull;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class TaperedCapsuleShape extends ConvexShape {
    private Vector3f centerOfMass;
    private float topRadius;
    private float bottomRadius;
    private float topCenter;
    private float bottomCenter;
    private float convexRadius;
    private float sinAlpha;
    private float tanAlpha;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);

        centerOfMass = JoltUtils.getAlignedVector3(buffer);
        topRadius = buffer.getFloat();
        bottomRadius = buffer.getFloat();
        topCenter = buffer.getFloat();
        bottomCenter = buffer.getFloat();
        convexRadius = buffer.getFloat();
        sinAlpha = buffer.getFloat();
        tanAlpha = buffer.getFloat();
    }
}
