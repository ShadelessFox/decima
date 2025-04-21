package com.shade.decima.game.hfw.data.jolt.physics.collision.shape;

import com.shade.decima.game.hfw.data.jolt.JoltUtils;
import com.shade.decima.game.hfw.data.jolt.math.Vec3;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class TaperedCapsuleShape extends ConvexShape {
    public Vec3 centerOfMass;
    public float topRadius;
    public float bottomRadius;
    public float topCenter;
    public float bottomCenter;
    public float convexRadius;
    public float sinAlpha;
    public float tanAlpha;

    @Override
    public void restoreBinaryState(@NotNull BinaryReader reader) throws IOException {
        super.restoreBinaryState(reader);

        centerOfMass = JoltUtils.readAlignedVector3(reader);
        topRadius = reader.readFloat();
        bottomRadius = reader.readFloat();
        topCenter = reader.readFloat();
        bottomCenter = reader.readFloat();
        convexRadius = reader.readFloat();
        sinAlpha = reader.readFloat();
        tanAlpha = reader.readFloat();
    }
}
