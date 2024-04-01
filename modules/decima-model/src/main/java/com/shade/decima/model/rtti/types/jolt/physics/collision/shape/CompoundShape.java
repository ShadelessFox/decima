package com.shade.decima.model.rtti.types.jolt.physics.collision.shape;

import com.shade.decima.model.rtti.types.jolt.JoltUtils;
import com.shade.decima.model.rtti.types.jolt.geometry.AABox;
import com.shade.util.NotNull;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class CompoundShape extends Shape {
    public static class SubShape {
        public final int userData;
        public final Vector3f position;
        public final Vector3f rotation;
        public Shape shape;

        public SubShape(int userData, @NotNull Vector3f position, @NotNull Vector3f rotation) {
            this.userData = userData;
            this.position = position;
            this.rotation = rotation;
        }

        @NotNull
        private static SubShape get(@NotNull ByteBuffer buffer) {
            final var userData = buffer.getInt();
            final var position = JoltUtils.getVector3(buffer);
            final var rotation = JoltUtils.getVector3(buffer);

            return new SubShape(userData, position, rotation);
        }
    }

    private Vector3f centerOfMass;
    private AABox localBounds;
    private float innerRadius;
    private SubShape[] subShapes;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);
        centerOfMass = JoltUtils.getAlignedVector3(buffer);
        localBounds = JoltUtils.getAABox(buffer);
        innerRadius = buffer.getFloat();
        subShapes = JoltUtils.getArray(buffer, SubShape[]::new, SubShape::get);
    }

    @Override
    public void restoreSubShapeState(@NotNull Shape[] subShapes) {
        assert this.subShapes.length == subShapes.length;
        for (int i = 0; i < subShapes.length; i++) {
            this.subShapes[i].shape = subShapes[i];
        }
    }
}
