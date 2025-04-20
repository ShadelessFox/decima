package com.shade.decima.game.hfw.data.jolt.physics.collision.shape;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class CylinderShape extends ConvexShape {
    public float halfHeight;
    public float radius;
    public float convexRadius;

    @Override
    public void restoreBinaryState(@NotNull BinaryReader reader) throws IOException {
        super.restoreBinaryState(reader);

        halfHeight = reader.readFloat();
        radius = reader.readFloat();
        convexRadius = reader.readFloat();
    }
}
