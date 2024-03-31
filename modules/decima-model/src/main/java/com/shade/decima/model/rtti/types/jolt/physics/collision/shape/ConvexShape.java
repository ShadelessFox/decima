package com.shade.decima.model.rtti.types.jolt.physics.collision.shape;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class ConvexShape extends Shape {
    private float density;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);

        density = buffer.getFloat();
    }
}
