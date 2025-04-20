package com.shade.decima.game.hfw.data.jolt.physics.constraints;

import com.shade.decima.game.hfw.data.jolt.core.Factory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class ConstraintSettings {
    public float drawConstraintSize;

    @NotNull
    public static ConstraintSettings restoreFromBinaryState(@NotNull BinaryReader reader) throws IOException {
        var hash = reader.readInt();
        var name = Factory.getTypeName(hash);
        var result = switch (name) {
            case "HingeConstraintSettings" -> new HingeConstraintSettings();
            case "PointConstraintSettings" -> new PointConstraintSettings();
            case "SwingTwistConstraintSettings" -> new SwingTwistConstraintSettings();
            case "SliderConstraintSettings" -> new SliderConstraintSettings();
            default -> throw new UnsupportedOperationException("Constraint %s not implemented".formatted(name));
        };
        result.restoreBinaryState(reader);
        return result;
    }

    public void restoreBinaryState(@NotNull BinaryReader reader) throws IOException {
        drawConstraintSize = reader.readFloat();
    }
}
