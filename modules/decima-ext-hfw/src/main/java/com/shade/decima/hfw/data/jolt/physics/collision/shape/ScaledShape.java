package com.shade.decima.hfw.data.jolt.physics.collision.shape;

import com.shade.decima.hfw.data.jolt.JoltUtils;
import com.shade.util.NotNull;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class ScaledShape extends DecoratedShape {
    private Vector3f scale;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);

        scale = JoltUtils.getAlignedVector3(buffer);
    }
}
