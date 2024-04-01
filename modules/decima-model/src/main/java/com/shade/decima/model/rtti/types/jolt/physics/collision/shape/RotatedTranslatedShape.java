package com.shade.decima.model.rtti.types.jolt.physics.collision.shape;

import com.shade.decima.model.rtti.types.jolt.JoltUtils;
import com.shade.util.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class RotatedTranslatedShape extends DecoratedShape {
    private Vector3f centerOfMass;
    private Quaternionf rotation;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);

        centerOfMass = JoltUtils.getAlignedVector3(buffer);
        rotation = JoltUtils.getQuaternion(buffer);
    }
}
