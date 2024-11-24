package com.shade.decima.hrzr.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class TextureCallback implements ExtraBinaryDataCallback<TextureCallback.TextureData> {
    public interface TextureData {
        @Attr(name = "Header", type = "Array<uint8>", position = 0, offset = 0)
        byte[] header();

        void header(byte[] value);

        @Attr(name = "Data", type = "Array<uint8>", position = 1, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull TextureData object) throws IOException {
        var header = reader.readBytes(32);
        var data = reader.readBytes(reader.readInt());
    }
}
