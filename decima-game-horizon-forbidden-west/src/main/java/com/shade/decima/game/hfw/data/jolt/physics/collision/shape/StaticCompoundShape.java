package com.shade.decima.game.hfw.data.jolt.physics.collision.shape;

import com.shade.decima.game.hfw.data.jolt.JoltUtils;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.List;

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
        private static Node read(@NotNull BinaryReader reader) throws IOException {
            var boundsMinX = reader.readShorts(4);
            var boundsMinY = reader.readShorts(4);
            var boundsMinZ = reader.readShorts(4);
            var boundsMaxX = reader.readShorts(4);
            var boundsMaxY = reader.readShorts(4);
            var boundsMaxZ = reader.readShorts(4);
            var nodeProperties = reader.readInts(4);

            return new Node(boundsMinX, boundsMinY, boundsMinZ, boundsMaxX, boundsMaxY, boundsMaxZ, nodeProperties);
        }
    }

    public List<Node> nodes;

    @Override
    public void restoreBinaryState(@NotNull BinaryReader reader) throws IOException {
        super.restoreBinaryState(reader);

        nodes = JoltUtils.readObjects(reader, Node::read);
    }
}
