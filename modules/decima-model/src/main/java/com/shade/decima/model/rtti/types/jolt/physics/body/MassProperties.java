package com.shade.decima.model.rtti.types.jolt.physics.body;

import com.shade.decima.model.rtti.types.jolt.JoltUtils;
import com.shade.util.NotNull;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

public record MassProperties(float mass, @NotNull Matrix4f inertia) {
    @NotNull
    public static MassProperties restoreFromBinaryState(@NotNull ByteBuffer buffer) {
        final var mass = buffer.getFloat();
        final var inertia = JoltUtils.getMatrix4(buffer);

        return new MassProperties(mass, inertia);
    }
}
