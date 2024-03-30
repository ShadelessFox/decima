package com.shade.decima.model.rtti.types.jolt.shape;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class SphereShape extends ConvexShape {
    private float radius;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);
        radius = buffer.getFloat();
    }
}
