package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class PhysicsCollisionResourceCallback implements ExtraBinaryDataCallback<PhysicsCollisionResourceCallback.HavokDataBlock> {
    public interface HavokDataBlock {
        @Attr(name = "Data", type = "Array<uint8>", position = 0, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull HavokDataBlock object) throws IOException {
        object.data(reader.readBytes(reader.readInt()));
    }
}
