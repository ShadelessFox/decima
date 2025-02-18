package com.shade.decima.game.hfw.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class MorphemeNetworkInstancePreInitializedDataCallback implements ExtraBinaryDataCallback<MorphemeNetworkInstancePreInitializedDataCallback.MorphemeNetworkInstancePreInitializedData> {
    public interface MorphemeNetworkInstancePreInitializedData {
        @Attr(name = "Unk1", type = "Array<uint8>", position = 0, offset = 0)
        byte[] unk1();

        void unk1(byte[] value);

        @Attr(name = "Unk2", type = "Array<uint32>", position = 1, offset = 0)
        int[] unk2();

        void unk2(int[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull MorphemeNetworkInstancePreInitializedData object) throws IOException {
        var count = reader.readInt();
        var four = reader.readInt(value -> value == 4, value -> "Value expected to be 4, was " + value);
        if (count > 0) {
            object.unk1(reader.readBytes(count));
            object.unk2(reader.readInts(reader.readInt()));
        }
    }
}
