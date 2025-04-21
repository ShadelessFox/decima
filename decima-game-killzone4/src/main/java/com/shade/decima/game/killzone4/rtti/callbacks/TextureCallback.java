package com.shade.decima.game.killzone4.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class TextureCallback implements ExtraBinaryDataCallback<TextureCallback.TextureInfo> {
    public interface TextureInfo {
        @Attr(name = "Header", type = "Array<uint8>", position = 0, offset = 0)
        byte[] header();

        void header(byte[] value);

        @Attr(name = "Data", type = "Array<uint8>", position = 1, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull TextureInfo object) throws IOException {
        object.header(reader.readBytes(40));
        object.data(reader.readBytes(reader.readInt()));
    }
}
