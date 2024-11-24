package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class HavokClothResourceCallback implements ExtraBinaryDataCallback<HavokClothResourceCallback.HavokData> {
    public interface HavokData {
        @Attr(name = "HavokData", type = "Array<uint8>", position = 0, offset = 0)
        byte[] havokData();

        void havokData(byte[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull HavokData object) throws IOException {
        object.havokData(reader.readBytes(reader.readInt()));
    }
}
