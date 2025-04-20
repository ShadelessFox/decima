package com.shade.decima.game.hfw.rtti.callbacks;

import com.shade.decima.game.hfw.data.jolt.physics.collision.PhysicsMaterial;
import com.shade.decima.game.hfw.data.jolt.physics.collision.shape.Shape;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhysicsShapeResourceCallback implements ExtraBinaryDataCallback<PhysicsShapeResourceCallback.PhysicsShapeData> {
    public interface PhysicsShapeData {
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull PhysicsShapeData object) throws IOException {
        var shapeMap = new ArrayList<Shape>();
        shapeMap.add(null);

        // FIXME: Materials must be derived from the object
        var materialMap = new ArrayList<PhysicsMaterial>();
        materialMap.add(null);

        // FIXME: Skipped for now
        var shape = restoreFromBinaryState(reader, shapeMap, materialMap);
    }

    @Nullable
    private static Shape restoreFromBinaryState(
        @NotNull BinaryReader reader,
        @NotNull List<Shape> shapeMap,
        @NotNull List<PhysicsMaterial> materialMap
    ) throws IOException {
        var shapeId = reader.readInt();
        if (shapeId < shapeMap.size()) {
            return shapeMap.get(shapeId);
        }

        var shape = Shape.sRestoreFromBinaryState(reader);
        for (int i = shapeMap.size(); i < shapeId; i++) {
            shapeMap.add(null);
        }

        assert shapeId == shapeMap.size();
        shapeMap.add(shape);

        var children = new Shape[reader.readInt()];
        for (int i = 0; i < children.length; i++) {
            children[i] = restoreFromBinaryState(reader, shapeMap, materialMap);
        }

        var materials = new PhysicsMaterial[reader.readInt()];
        for (int i = 0; i < materials.length; i++) {
            int materialId = reader.readInt();
            if (materialId == ~0) {
                continue;
            }
            if (materialId < materialMap.size()) {
                materials[i] = materialMap.get(materialId);
            } else {
                // TODO: Materials are ignored for now
                // throw new NotImplementedException();
            }
        }

        shape.restoreSubShapeState(children);
        shape.restoreMaterialState(materials);

        return shape;
    }

}
