package com.shade.decima.model.rtti.types.jolt.physics.collision.shape;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

public class DecoratedShape extends Shape {
    private Shape innerShape;

    @Override
    public void restoreSubShapeState(@NotNull Shape[] subShapes) {
        assert subShapes.length == 1;
        innerShape = subShapes[0];
    }

    @Nullable
    public Shape getInnerShape() {
        return innerShape;
    }
}
