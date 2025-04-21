package com.shade.decima.game.hfw.data.jolt.physics.collision.shape;

import com.shade.decima.game.hfw.data.jolt.physics.collision.PhysicsMaterial;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.List;

public class Shape {
    protected int mUserData;

    @NotNull
    public static Shape sRestoreFromBinaryState(@NotNull BinaryReader reader) throws IOException {
        var shapeSubType = ShapeSubType.values()[reader.readByte()];
        var shape = ShapeFunctions.get(shapeSubType).construct();
        shape.restoreBinaryState(reader);

        return shape;
    }

    @Nullable
    public static Shape sRestoreWithChildren(
        @NotNull BinaryReader reader,
        @NotNull List<Shape> shapeMap,
        @NotNull List<PhysicsMaterial> materialMap
    ) throws IOException {
        var shapeId = reader.readInt();
        if (shapeId == ~0) {
            return null;
        }
        throw new NotImplementedException();
    }

    public void restoreBinaryState(@NotNull BinaryReader reader) throws IOException {
        mUserData = reader.readInt();
    }

    public void restoreMaterialState(@NotNull PhysicsMaterial[] materials) {
        assert materials.length == 0;
    }

    public void restoreSubShapeState(@NotNull Shape[] subShapes) {
        assert subShapes.length == 0;
    }
}
