package com.shade.decima.game.killzone3.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class PoseCallback implements ExtraBinaryDataCallback<PoseCallback.PoseData> {
    public interface PoseData {
        @Attr(name = "Matrix0", type = "Array<uint8>", position = 0, offset = 0)
        byte[] matrix0();

        void matrix0(byte[] matrix0);

        @Attr(name = "Matrix1", type = "Array<uint8>", position = 1, offset = 0)
        byte[] matrix1();

        void matrix1(byte[] matrix1);

        @Attr(name = "Floats", type = "Array<float>", position = 2, offset = 0)
        float[] floats();

        void floats(float[] floats);
    }

    @Override
    public void deserialize(BinaryReader reader, TypeFactory factory, PoseData object) throws IOException {
        if (!reader.readByteBoolean()) {
            return;
        }

        int numMatrices = reader.readInt();
        object.matrix0(reader.readBytes(48 * numMatrices));
        object.matrix1(reader.readBytes(64 * numMatrices));

        var numFloats = reader.readInt();
        object.floats(reader.readFloats(numFloats));
    }
}
