package com.shade.decima.model.rtti.types.jolt.physics.collision.shape;

import com.shade.decima.model.rtti.types.jolt.JoltUtils;
import com.shade.util.NotNull;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class BoxShape extends ConvexShape {
    private Vector3f halfExtent;
    private float convexRadius;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);

        halfExtent = JoltUtils.getAlignedVector3(buffer);
        convexRadius = buffer.getFloat();
    }
}
