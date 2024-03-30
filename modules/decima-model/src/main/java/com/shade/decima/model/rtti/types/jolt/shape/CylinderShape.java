package com.shade.decima.model.rtti.types.jolt.shape;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class CylinderShape extends ConvexShape {
    private float halfHeight;
    private float radius;
    private float convexRadius;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);
        halfHeight = buffer.getFloat();
        radius = buffer.getFloat();
        convexRadius = buffer.getFloat();
    }
}
