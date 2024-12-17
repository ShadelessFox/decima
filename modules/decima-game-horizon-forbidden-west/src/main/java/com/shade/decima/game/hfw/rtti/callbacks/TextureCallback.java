package com.shade.decima.game.hfw.rtti.callbacks;

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

        @NotNull
        static TextureData read(@NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
            var data = factory.newInstance(TextureData.class);
            new TextureCallback().deserialize(reader, factory, data);
            return data;
        }
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull TextureData object) throws IOException {
        object.header(reader.readBytes(32));
        object.data(reader.readBytes(reader.readInt()));
    }
}