package com.shade.decima.model.rtti.types.jolt.skeleton;

import com.shade.decima.model.rtti.types.jolt.JoltUtils;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public record Skeleton(@NotNull Joint[] joints) {
    public record Joint(@NotNull String name, @NotNull String parentName, int parentJointIndex) {
        @NotNull
        private static Joint get(@NotNull ByteBuffer buffer) {
            final var name = JoltUtils.getString(buffer);
            final var parentJointIndex = buffer.getInt();
            final var parentName = JoltUtils.getString(buffer);

            return new Joint(name, parentName, parentJointIndex);
        }
    }

    @NotNull
    public static Skeleton restoreFromBinaryState(@NotNull ByteBuffer buffer) {
        final var joints = BufferUtils.getObjects(buffer, buffer.getInt(), Joint[]::new, Joint::get);

        return new Skeleton(joints);
    }
}
