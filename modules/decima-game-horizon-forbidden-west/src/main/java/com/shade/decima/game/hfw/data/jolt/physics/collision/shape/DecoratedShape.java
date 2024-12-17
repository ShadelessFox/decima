package com.shade.decima.game.hfw.data.jolt.physics.collision.shape;

import com.shade.util.NotNull;

public class DecoratedShape extends Shape {
    public Shape innerShape;

    @Override
    public void restoreSubShapeState(@NotNull Shape[] subShapes) {
        assert subShapes.length == 1;
        innerShape = subShapes[0];
    }
}
