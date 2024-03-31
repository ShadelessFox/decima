package com.shade.decima.model.rtti.types.jolt;

import com.shade.decima.model.rtti.types.jolt.geometry.AABox;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.util.function.Function;
import java.util.function.IntFunction;

public class JoltUtils {
    private JoltUtils() {
        // prevents instantiation
    }

    @NotNull
    public static Vector3f getVector3(@NotNull ByteBuffer buffer) {
        final float x = buffer.getFloat();
        final float y = buffer.getFloat();
        final float z = buffer.getFloat();

        return new Vector3f(x, y, z);
    }

    @NotNull
    public static Vector3f getAlignedVector3(@NotNull ByteBuffer buffer) {
        final float x = buffer.getFloat();
        final float y = buffer.getFloat();
        final float z = buffer.getFloat();
        final float w = buffer.getFloat();

        if (z != w) {
            throw new IllegalArgumentException("z and w must be equal");
        }

        return new Vector3f(x, y, z);
    }

    @NotNull
    public static Quaternionf getQuaternion(@NotNull ByteBuffer buffer) {
        final float x = buffer.getFloat();
        final float y = buffer.getFloat();
        final float z = buffer.getFloat();
        final float w = buffer.getFloat();

        return new Quaternionf(x, y, z, w);
    }

    @NotNull
    public static Matrix4f getMatrix4(@NotNull ByteBuffer buffer) {
        return new Matrix4f(
            buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat(),
            buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat(),
            buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat(),
            buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat()
        );
    }

    @NotNull
    public static AABox getAABox(@NotNull ByteBuffer buffer) {
        return new AABox(getAlignedVector3(buffer), getAlignedVector3(buffer));
    }

    @NotNull
    public static <T> T[] getArray(@NotNull ByteBuffer buffer, @NotNull IntFunction<T[]> generator, @NotNull Function<ByteBuffer, T> reader) {
        return BufferUtils.getObjects(buffer, Math.toIntExact(buffer.getLong()), generator, reader);
    }

    @NotNull
    public static String getString(@NotNull ByteBuffer buffer) {
        return BufferUtils.getString(buffer, Math.toIntExact(buffer.getLong()));
    }
}
