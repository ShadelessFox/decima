package com.shade.decima.hrzr.rtti.callbacks;

import com.shade.decima.rtti.TypeFactory;
import com.shade.decima.rtti.data.meta.Attr;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class PhysicsShapeResourceCallback implements ExtraBinaryDataCallback<PhysicsShapeResourceCallback.PhysicsShapeData> {
    public interface PhysicsShapeData {
        @Attr(name = "Data", type = "Array<uint8>", position = 0, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull PhysicsShapeData object) throws IOException {
        object.data(reader.readBytes(reader.readInt()));
    }
}
