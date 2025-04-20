package com.shade.decima.game.hfw.data.jolt;

import com.shade.decima.game.hfw.data.jolt.geometry.AABox;
import com.shade.decima.game.hfw.data.jolt.math.Mat44;
import com.shade.decima.game.hfw.data.jolt.math.Quat;
import com.shade.decima.game.hfw.data.jolt.math.Vec3;
import com.shade.decima.game.hfw.data.jolt.math.Vec4;
import com.shade.util.NotNull;
import com.shade.util.ThrowableFunction;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.List;

public final class JoltUtils {
    private JoltUtils() {
        // prevents instantiation
    }

    @NotNull
    public static Vec3 readVec3(@NotNull BinaryReader reader) throws IOException {
        var x = reader.readFloat();
        var y = reader.readFloat();
        var z = reader.readFloat();

        return new Vec3(x, y, z);
    }

    @NotNull
    public static Vec3 readAlignedVector3(@NotNull BinaryReader reader) throws IOException {
        var x = reader.readFloat();
        var y = reader.readFloat();
        var z = reader.readFloat();
        var w = reader.readFloat();

        if (z != w) {
            throw new IllegalArgumentException("z and w must be equal");
        }

        return new Vec3(x, y, z);
    }

    @NotNull
    public static Vec4 readVec4(@NotNull BinaryReader reader) throws IOException {
        var x = reader.readFloat();
        var y = reader.readFloat();
        var z = reader.readFloat();
        var w = reader.readFloat();

        return new Vec4(x, y, z, w);
    }

    @NotNull
    public static Quat readQuaternion(@NotNull BinaryReader reader) throws IOException {
        var x = reader.readFloat();
        var y = reader.readFloat();
        var z = reader.readFloat();
        var w = reader.readFloat();

        return new Quat(x, y, z, w);
    }

    @NotNull
    public static Mat44 readMatrix4(@NotNull BinaryReader reader) throws IOException {
        var col0 = readVec4(reader);
        var col1 = readVec4(reader);
        var col2 = readVec4(reader);
        var col3 = readVec4(reader);

        return new Mat44(col0, col1, col2, col3);
    }

    @NotNull
    public static AABox readAABox(@NotNull BinaryReader reader) throws IOException {
        return new AABox(readAlignedVector3(reader), readAlignedVector3(reader));
    }

    @NotNull
    public static <T> List<T> readObjects(@NotNull BinaryReader reader, @NotNull ThrowableFunction<BinaryReader, T, IOException> mapper) throws IOException {
        return reader.readObjects(readCount(reader), mapper);
    }

    @NotNull
    public static byte[] readBytes(@NotNull BinaryReader reader) throws IOException {
        return reader.readBytes(readCount(reader));
    }

    @NotNull
    public static String readString(@NotNull BinaryReader reader) throws IOException {
        return reader.readString(readCount(reader));
    }

    private static int readCount(@NotNull BinaryReader reader) throws IOException {
        return Math.toIntExact(reader.readLong());
    }
}
