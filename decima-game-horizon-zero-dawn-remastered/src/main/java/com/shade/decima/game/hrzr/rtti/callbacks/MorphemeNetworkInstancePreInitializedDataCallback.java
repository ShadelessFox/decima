package com.shade.decima.game.hrzr.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class MorphemeNetworkInstancePreInitializedDataCallback implements ExtraBinaryDataCallback<MorphemeNetworkInstancePreInitializedDataCallback.MorphemeNetworkInstanceData> {
    public interface MorphemeNetworkInstanceData {
        @Attr(name = "Data", type = "Array<uint8>", position = 0, offset = 0)
        byte[] data();

        void data(byte[] value);

        @Attr(name = "Unknown", type = "Array<uint32>", position = 1, offset = 0)
        int[] unknown();

        void unknown(int[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull MorphemeNetworkInstanceData object) throws IOException {
        object.data(reader.readBytes((int) reader.readLong()));
        object.unknown(reader.readInts(reader.readInt()));
    }
}
