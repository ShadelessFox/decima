package com.shade.decima.hfw.rtti.messages;

import com.shade.decima.hfw.data.jolt.physics.collision.PhysicsMaterial;
import com.shade.decima.hfw.data.jolt.physics.collision.shape.Shape;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "PhysicsShapeResource", game = GameType.HFW),
})
public class HFWPhysicsShapeResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
        final List<Shape> shapeMap = new ArrayList<>();
        shapeMap.add(null);

        // TODO: Materials must be derived from the object
        final List<PhysicsMaterial> materialMap = new ArrayList<>();
        materialMap.add(null);

        // TODO: Skipped for now
        final var shape = restoreFromBinaryState(buffer, shapeMap, materialMap);
    }

    @Override
    public void write(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        throw new NotImplementedException();
    }

    @Override
    public int getSize(@NotNull RTTIObject object, @NotNull RTTIFactory factory) {
        throw new NotImplementedException();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTIFactory factory) {
        return new Component[0];
    }

    @Nullable
    private static Shape restoreFromBinaryState(@NotNull ByteBuffer buffer, @NotNull List<Shape> shapeMap, @NotNull List<PhysicsMaterial> materialMap) {
        final var shapeId = buffer.getInt();
        if (shapeId < shapeMap.size()) {
            return shapeMap.get(shapeId);
        }

        final Shape shape = Shape.sRestoreFromBinaryState(buffer);
        for (int i = shapeMap.size(); i < shapeId; i++) {
            shapeMap.add(null);
        }

        assert shapeId == shapeMap.size();
        shapeMap.add(shape);

        final Shape[] children = new Shape[buffer.getInt()];
        for (int i = 0; i < children.length; i++) {
            children[i] = restoreFromBinaryState(buffer, shapeMap, materialMap);
        }

        final PhysicsMaterial[] materials = new PhysicsMaterial[buffer.getInt()];
        for (int i = 0; i < materials.length; i++) {
            final int materialId = buffer.getInt();
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
