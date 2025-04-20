package com.shade.decima.game.hfw.data.jolt.physics.body;

import com.shade.decima.game.hfw.data.jolt.JoltUtils;
import com.shade.decima.game.hfw.data.jolt.math.Mat44;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public record MassProperties(float mass, @NotNull Mat44 inertia) {
    @NotNull
    public static MassProperties restoreFromBinaryState(@NotNull BinaryReader reader) throws IOException {
        var mass = reader.readFloat();
        var inertia = JoltUtils.readMatrix4(reader);

        return new MassProperties(mass, inertia);
    }
}
