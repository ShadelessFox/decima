package com.shade.decima.game.hfw.data.jolt.physics.collision.shape;

import com.shade.decima.game.hfw.data.jolt.physics.collision.PhysicsMaterial;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class ConvexShape extends Shape {
    public float density;
    public PhysicsMaterial material;

    @Override
    public void restoreBinaryState(@NotNull BinaryReader reader) throws IOException {
        super.restoreBinaryState(reader);

        density = reader.readFloat();
    }

    @Override
    public void restoreMaterialState(@NotNull PhysicsMaterial[] materials) {
        assert materials.length == 1;
        material = materials[0];
    }
}
