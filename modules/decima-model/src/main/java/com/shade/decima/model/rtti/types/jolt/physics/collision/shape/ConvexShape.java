package com.shade.decima.model.rtti.types.jolt.physics.collision.shape;

import com.shade.decima.model.rtti.types.jolt.physics.collision.PhysicsMaterial;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class ConvexShape extends Shape {
    private float density;
    private PhysicsMaterial material;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);

        density = buffer.getFloat();
    }

    @Override
    public void restoreMaterialState(@NotNull PhysicsMaterial[] materials) {
        assert materials.length == 1;
        material = materials[0];
    }
}
