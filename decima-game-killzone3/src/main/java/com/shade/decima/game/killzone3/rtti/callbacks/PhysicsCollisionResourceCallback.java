package com.shade.decima.game.killzone3.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class PhysicsCollisionResourceCallback implements ExtraBinaryDataCallback<PhysicsCollisionResourceCallback.HavokDataBlock> {
    public interface HavokDataBlock {
        @Attr(name = "Data", type = "Array<uint8>", position = 0, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(BinaryReader reader, TypeFactory factory, HavokDataBlock object) throws IOException {
        var size = reader.readInt();
        reader.align(16);
        object.data(reader.readBytes(size));
        reader.align(4);
    }
}
