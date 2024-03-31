package com.shade.decima.model.rtti.types.jolt.physics.collision;

import com.shade.decima.model.rtti.types.jolt.core.Factory;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class GroupFilter {
    @NotNull
    public static GroupFilter sRestoreFromBinaryState(@NotNull ByteBuffer buffer) {
        final var hash = buffer.getInt();
        final var name = Factory.getTypeName(hash);
        final var result = switch (name) {
            case "GroupFilterTable" -> new GroupFilterTable();
            default -> throw new NotImplementedException();
        };
        result.restoreBinaryState(buffer);
        return result;
    }

    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        // no-op
    }
}
