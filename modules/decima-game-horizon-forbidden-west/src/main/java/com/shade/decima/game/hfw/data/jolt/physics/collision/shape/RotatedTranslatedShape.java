package com.shade.decima.game.hfw.data.jolt.physics.collision.shape;

import com.shade.decima.game.hfw.data.jolt.JoltUtils;
import com.shade.decima.game.hfw.data.jolt.math.Quat;
import com.shade.decima.game.hfw.data.jolt.math.Vec3;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class RotatedTranslatedShape extends DecoratedShape {
    public Vec3 centerOfMass;
    public Quat rotation;

    @Override
    public void restoreBinaryState(@NotNull BinaryReader reader) throws IOException {
        super.restoreBinaryState(reader);

        centerOfMass = JoltUtils.readAlignedVector3(reader);
        rotation = JoltUtils.readQuaternion(reader);
    }
}
