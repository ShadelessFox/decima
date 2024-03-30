package com.shade.decima.model.rtti.types.jolt.shape;

import com.shade.decima.model.rtti.types.jolt.JoltUtils;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class StaticCompoundShape extends CompoundShape {
    public record Node(
        @NotNull short[] boundsMinX,
        @NotNull short[] boundsMinY,
        @NotNull short[] boundsMinZ,
        @NotNull short[] boundsMaxX,
        @NotNull short[] boundsMaxY,
        @NotNull short[] boundsMaxZ,
        @NotNull int[] nodeProperties
    ) {
        @NotNull
        private static Node get(@NotNull ByteBuffer buffer) {
            final var boundsMinX = BufferUtils.getShorts(buffer, 4);
            final var boundsMinY = BufferUtils.getShorts(buffer, 4);
            final var boundsMinZ = BufferUtils.getShorts(buffer, 4);
            final var boundsMaxX = BufferUtils.getShorts(buffer, 4);
            final var boundsMaxY = BufferUtils.getShorts(buffer, 4);
            final var boundsMaxZ = BufferUtils.getShorts(buffer, 4);
            final var nodeProperties = BufferUtils.getInts(buffer, 4);

            return new Node(boundsMinX, boundsMinY, boundsMinZ, boundsMaxX, boundsMaxY, boundsMaxZ, nodeProperties);
        }
    }

    private Node[] nodes;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);
        nodes = JoltUtils.getArray(buffer, Node[]::new, Node::get);
    }
}
