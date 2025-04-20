package com.shade.decima.game.hfw.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.List;

public class TextureListCallback implements ExtraBinaryDataCallback<TextureListCallback.TextureListData> {
    public interface TextureListData {
        @Attr(name = "Entries", type = "Array<TextureEntry>", position = 0, offset = 0)
        List<TextureEntry> entries();

        void entries(List<TextureEntry> value);
    }

    public interface TextureEntry {
        @Attr(name = "StreamingOffset", type = "uint32", position = 0, offset = 0)
        int streamingOffset();

        void streamingOffset(int value);

        @Attr(name = "StreamingLength", type = "uint32", position = 1, offset = 0)
        int streamingLength();

        void streamingLength(int value);

        @Attr(name = "Data", type = "TextureData", position = 2, offset = 0)
        TextureCallback.TextureData data();

        void data(TextureCallback.TextureData value);

        @NotNull
        static TextureEntry read(@NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
            var entry = factory.newInstance(TextureEntry.class);
            entry.streamingOffset(reader.readInt());
            entry.streamingLength(reader.readInt());
            entry.data(TextureCallback.TextureData.read(reader, factory));
            return entry;
        }
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull TextureListData object) throws IOException {
        object.entries(reader.readObjects(reader.readInt(), r -> TextureEntry.read(r, factory)));
    }
}
