package com.shade.decima.hrzr.rtti.callbacks;

import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class TextureListCallback implements ExtraBinaryDataCallback<TextureListCallback.TextureListData> {
    public interface TextureListData {

    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull TextureListData object) throws IOException {
        var count = reader.readInt();

        for (int i = 0; i < count; i++) {
            var dataOffset = reader.readInt();
            var dataLength = reader.readInt();

            var texture = factory.newInstance(TextureCallback.TextureData.class);
            texture.header(reader.readBytes(32));
            texture.data(reader.readBytes(reader.readInt()));

            continue;
        }
    }
}
