package com.shade.decima.model.rtti.messages.hfw;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.jolt.shape.Shape;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "PhysicsShapeResource", game = GameType.HFW),
})
public class HFWPhysicsShapeResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        // TODO: Skipped for now
        final SerializedShape shape = SerializedShape.read(buffer);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        throw new NotImplementedException();
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object) {
        throw new NotImplementedException();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[0];
    }

    private record SerializedShape(int id, @NotNull Shape shape, @NotNull SerializedShape[] children, @NotNull int[] values) {
        @Nullable
        public static SerializedShape read(@NotNull ByteBuffer buffer) {
            final var id = buffer.getInt();
            if (id == 0) {
                return null;
            }
            final var shape = Shape.restoreFromBinaryState(buffer);
            final var children = BufferUtils.getObjects(buffer, buffer.getInt(), SerializedShape[]::new, SerializedShape::read);
            final var values = BufferUtils.getInts(buffer, buffer.getInt());
            return new SerializedShape(id, shape, children, values);
        }
    }
}
