package com.shade.decima.game.killzone4.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
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
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull PoseData object) throws IOException {
        if (!reader.readByteBoolean()) {
            return;
        }

        int numMatrices = reader.readInt();
        var matrix0 = reader.readBytes(48 * numMatrices);
        var matrix1 = reader.readBytes(64 * numMatrices);

        var numFloats = reader.readInt();
        var floats = reader.readFloats(numFloats);

        object.matrix0(matrix0);
        object.matrix1(matrix1);
        object.floats(floats);
    }
}
