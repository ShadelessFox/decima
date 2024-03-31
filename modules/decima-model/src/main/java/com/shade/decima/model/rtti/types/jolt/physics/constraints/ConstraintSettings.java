package com.shade.decima.model.rtti.types.jolt.physics.constraints;

import com.shade.decima.model.rtti.types.jolt.core.Factory;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class ConstraintSettings {
    private float drawConstraintSize;

    @NotNull
    public static ConstraintSettings restoreFromBinaryState(@NotNull ByteBuffer buffer) {
        final var hash = buffer.getInt();
        final var name = Factory.getTypeName(hash);
        final var result = switch (name) {
            case "SwingTwistConstraintSettings" -> new SwingTwistConstraintSettings();
            default -> throw new NotImplementedException();
        };
        result.restoreBinaryState(buffer);
        return result;
    }

    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        drawConstraintSize = buffer.getFloat();
    }
}
