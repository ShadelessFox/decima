package com.shade.decima.model.rtti.types.jolt.physics.collision.shape;

import com.shade.decima.model.rtti.types.jolt.JoltUtils;
import com.shade.decima.model.rtti.types.jolt.geometry.AABox;
import com.shade.decima.model.rtti.types.jolt.geometry.Plane;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ConvexHullShape extends ConvexShape {
    public record Point(@NotNull Vector3f position, @NotNull int[] faces) {
        @NotNull
        private static Point get(@NotNull ByteBuffer buffer) {
            final var position = JoltUtils.getAlignedVector3(buffer);
            final var facesCount = buffer.getInt();
            final var faces = BufferUtils.getInts(buffer, 3);
            return new Point(position, Arrays.copyOf(faces, facesCount));
        }
    }

    public record Face(short firstVertex, short vertexCount) {
        @NotNull
        private static Face get(@NotNull ByteBuffer buffer) {
            return new Face(buffer.getShort(), buffer.getShort());
        }
    }

    private Vector3f centerOfMass;
    private Matrix4f inertia;
    private AABox localBounds;
    private Point[] points;
    private Face[] faces;
    private Plane[] planes;
    private byte[] vertexIdx;
    private float convexRadius;
    private float volume;
    private float innerRadius;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);

        centerOfMass = JoltUtils.getAlignedVector3(buffer);
        inertia = JoltUtils.getMatrix4(buffer);
        localBounds = JoltUtils.getAABox(buffer);
        points = JoltUtils.getArray(buffer, Point[]::new, Point::get);
        faces = JoltUtils.getArray(buffer, Face[]::new, Face::get);
        planes = JoltUtils.getArray(buffer, Plane[]::new, buf -> new Plane(JoltUtils.getVector3(buf), buf.getFloat()));
        vertexIdx = BufferUtils.getBytes(buffer, Math.toIntExact(buffer.getLong()));
        convexRadius = buffer.getFloat();
        volume = buffer.getFloat();
        innerRadius = buffer.getFloat();
    }
}
