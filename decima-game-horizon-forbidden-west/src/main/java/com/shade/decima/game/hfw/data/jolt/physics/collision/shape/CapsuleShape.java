package com.shade.decima.game.hfw.data.jolt.physics.collision.shape;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class CapsuleShape extends ConvexShape {
    public float radius;
    public float halfHeightOfCylinder;

    @Override
    public void restoreBinaryState(@NotNull BinaryReader reader) throws IOException {
        super.restoreBinaryState(reader);

        radius = reader.readFloat();
        halfHeightOfCylinder = reader.readFloat();
    }
}
