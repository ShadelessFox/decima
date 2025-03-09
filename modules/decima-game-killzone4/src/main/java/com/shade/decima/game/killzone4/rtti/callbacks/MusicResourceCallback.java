package com.shade.decima.game.killzone4.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class MusicResourceCallback implements ExtraBinaryDataCallback<MusicResourceCallback.ShaderData> {
    public interface ShaderData {
        @Attr(name = "Data", type = "Array<uint8>", position = 1, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull ShaderData object) throws IOException {
        object.data(reader.readBytes(reader.readInt()));
    }
}
