package com.shade.decima.model.rtti.types.jolt.physics.collision.shape;

import com.shade.decima.model.rtti.types.jolt.physics.collision.PhysicsMaterial;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class MeshShape extends Shape {
    private byte[] tree;
    private PhysicsMaterial[] materials;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);

        tree = BufferUtils.getBytes(buffer, Math.toIntExact(buffer.getLong()));
    }

    @Override
    public void restoreMaterialState(@NotNull PhysicsMaterial[] materials) {
        this.materials = materials;
    }
}
