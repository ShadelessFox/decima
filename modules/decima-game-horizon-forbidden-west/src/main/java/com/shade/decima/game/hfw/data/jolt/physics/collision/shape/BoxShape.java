package com.shade.decima.game.hfw.data.jolt.physics.collision.shape;

import com.shade.decima.game.hfw.data.jolt.JoltUtils;
import com.shade.decima.game.hfw.data.jolt.math.Vec3;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class BoxShape extends ConvexShape {
    public Vec3 halfExtent;
    public float convexRadius;

    @Override
    public void restoreBinaryState(@NotNull BinaryReader reader) throws IOException {
        super.restoreBinaryState(reader);

        halfExtent = JoltUtils.readAlignedVector3(reader);
        convexRadius = reader.readFloat();
    }
}
