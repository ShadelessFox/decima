package com.shade.decima.hfw.data.jolt.physics.collision.shape;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class CapsuleShape extends ConvexShape {
    private float radius;
    private float halfHeightOfCylinder;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);

        radius = buffer.getFloat();
        halfHeightOfCylinder = buffer.getFloat();
    }
}
