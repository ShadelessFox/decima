package com.shade.decima.hrzr.rtti.callbacks;

import com.shade.decima.rtti.TypeFactory;
import com.shade.decima.rtti.data.meta.Attr;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
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
