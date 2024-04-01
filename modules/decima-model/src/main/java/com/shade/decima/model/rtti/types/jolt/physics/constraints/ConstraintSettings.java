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
            case "SliderConstraintSettings" -> new SliderConstraintSettings();
            case "HingeConstraintSettings" -> new HingeConstraintSettings();
            case "PointConstraintSettings" -> new PointConstraintSettings();
            default -> throw new UnsupportedOperationException("Constraint %s not implemented".formatted(name));
        };
        result.restoreBinaryState(buffer);
        return result;
    }

    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        drawConstraintSize = buffer.getFloat();
    }
}
