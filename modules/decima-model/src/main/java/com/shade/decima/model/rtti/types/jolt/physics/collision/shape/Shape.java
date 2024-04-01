package com.shade.decima.model.rtti.types.jolt.physics.collision.shape;

import com.shade.decima.model.rtti.types.jolt.physics.collision.PhysicsMaterial;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.nio.ByteBuffer;
import java.util.List;

public class Shape {
    protected int mUserData;

    @NotNull
    public static Shape sRestoreFromBinaryState(@NotNull ByteBuffer buffer) {
        final ShapeSubType shapeSubType = ShapeSubType.values()[buffer.get()];
        final Shape shape = ShapeFunctions.get(shapeSubType).construct();
        shape.restoreBinaryState(buffer);
        return shape;
    }

    @Nullable
    public static Shape sRestoreWithChildren(
        @NotNull ByteBuffer buffer,
        @NotNull List<Shape> shapeMap,
        @NotNull List<PhysicsMaterial> materialMap
    ) {
        final var shapeId = buffer.getInt();
        if (shapeId == ~0) {
            return null;
        }
        throw new NotImplementedException();
    }

    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        mUserData = buffer.getInt();
    }

    public void restoreMaterialState(@NotNull PhysicsMaterial[] materials) {
        assert materials.length == 0;
    }

    public void restoreSubShapeState(@NotNull Shape[] subShapes) {
        assert subShapes.length == 0;
    }
}
