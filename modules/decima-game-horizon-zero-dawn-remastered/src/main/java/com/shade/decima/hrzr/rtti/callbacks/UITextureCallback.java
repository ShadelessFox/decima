package com.shade.decima.hrzr.rtti.callbacks;

import com.shade.decima.rtti.TypeFactory;
import com.shade.decima.rtti.data.meta.Attr;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class UITextureCallback implements ExtraBinaryDataCallback<UITextureCallback.UITextureData> {
    public interface UITextureData {
        @Attr(name = "SmallTexture", type = "TextureData", position = 0, offset = 0)
        TextureCallback.TextureData smallTexture();

        void smallTexture(TextureCallback.TextureData value);

        @Attr(name = "LargeTexture", type = "TextureData", position = 1, offset = 0)
        TextureCallback.TextureData largeTexture();

        void largeTexture(TextureCallback.TextureData value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull UITextureData object) throws IOException {
        var smallTextureSize = reader.readInt();
        var largeTextureSize = reader.readInt();

        if (smallTextureSize > 0) {
            var smallTexture = factory.newInstance(TextureCallback.TextureData.class);
            smallTexture.header(reader.readBytes(32));
            smallTexture.data(reader.readBytes(reader.readInt()));
            object.smallTexture(smallTexture);
        }

        if (largeTextureSize > 0) {
            var largeTexture = factory.newInstance(TextureCallback.TextureData.class);
            largeTexture.header(reader.readBytes(32));
            largeTexture.data(reader.readBytes(reader.readInt()));
            object.largeTexture(largeTexture);
        }
    }
}
