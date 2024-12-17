package com.shade.decima.game.hfw.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
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

        @Attr(name = "SmallFrames", type = "FramesData", position = 2, offset = 0)
        UITextureFramesCallback.FramesData smallFrames();

        void smallFrames(UITextureFramesCallback.FramesData value);

        @Attr(name = "LargeFrames", type = "FramesData", position = 3, offset = 0)
        UITextureFramesCallback.FramesData largeFrames();

        void largeFrames(UITextureFramesCallback.FramesData value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull UITextureData object) throws IOException {
        var framed = reader.readByteBoolean();
        var smallTextureSize = reader.readInt();
        var largeTextureSize = reader.readInt();

        if (framed) {
            if (smallTextureSize > 0) {
                object.smallFrames(UITextureFramesCallback.FramesData.read(reader, factory));
            }
            if (largeTextureSize > 0) {
                object.largeFrames(UITextureFramesCallback.FramesData.read(reader, factory));
            }
        } else {
            if (smallTextureSize > 0) {
                object.smallTexture(TextureCallback.TextureData.read(reader, factory));
            }
            if (largeTextureSize > 0) {
                object.largeTexture(TextureCallback.TextureData.read(reader, factory));
            }
        }
    }
}
