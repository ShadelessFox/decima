package com.shade.decima.game.hfw.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class MorphemeAssetCallback implements ExtraBinaryDataCallback<MorphemeAssetCallback.MorphemeAssetData> {
    public interface MorphemeAssetData {
        @Attr(name = "Data", type = "Array<uint8>", position = 0, offset = 0)
        byte[] data();

        void data(byte[] value);

        @Attr(name = "Unk02", type = "uint32", position = 1, offset = 0)
        int unk02();

        void unk02(int value);

        @Attr(name = "Unk03", type = "uint32", position = 2, offset = 0)
        int unk03();

        void unk03(int value);

        @Attr(name = "Unk04", type = "uint64", position = 3, offset = 0)
        long unk04();

        void unk04(long value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull MorphemeAssetData object) throws IOException {
        object.data(reader.readBytes(reader.readInt()));
        object.unk02(reader.readInt());
        object.unk03(reader.readInt());
        object.unk04(reader.readLong());
    }
}
