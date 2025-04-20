package com.shade.decima.game.hfw.data.jolt.physics.collision.shape;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class SphereShape extends ConvexShape {
    public float radius;

    @Override
    public void restoreBinaryState(@NotNull BinaryReader reader) throws IOException {
        super.restoreBinaryState(reader);

        radius = reader.readFloat();
    }
}
