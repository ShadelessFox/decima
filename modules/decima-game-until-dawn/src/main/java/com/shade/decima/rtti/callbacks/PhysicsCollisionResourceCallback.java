package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.RTTI;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class PhysicsCollisionResourceCallback implements ExtraBinaryDataCallback<PhysicsCollisionResourceCallback.HavokDataBlock> {
    public interface HavokDataBlock {
        @RTTI.Attr(name = "Data", type = "Array<uint8>", position = 0, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(@NotNull ByteBuffer buffer, @NotNull HavokDataBlock object) {
        object.data(BufferUtils.getBytes(buffer, buffer.getInt()));
    }
}
