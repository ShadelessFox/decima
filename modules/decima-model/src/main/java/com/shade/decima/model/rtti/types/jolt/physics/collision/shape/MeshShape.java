package com.shade.decima.model.rtti.types.jolt.physics.collision.shape;

import com.shade.decima.model.rtti.types.jolt.JoltUtils;
import com.shade.decima.model.rtti.types.jolt.physics.collision.PhysicsMaterial;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class MeshShape extends Shape {
    private byte[] tree;
    private PhysicsMaterial[] materials;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);

        tree = JoltUtils.getByteArray(buffer);
    }

    @Override
    public void restoreMaterialState(@NotNull PhysicsMaterial[] materials) {
        this.materials = materials;
    }
}
