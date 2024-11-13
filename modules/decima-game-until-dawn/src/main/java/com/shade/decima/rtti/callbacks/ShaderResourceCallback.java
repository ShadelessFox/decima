package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.TypeFactory;
import com.shade.decima.rtti.data.meta.Attr;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class ShaderResourceCallback implements ExtraBinaryDataCallback<ShaderResourceCallback.ShaderData> {
    public interface ShaderData {
        @Attr(name = "Data", type = "Array<uint8>", position = 0, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull ShaderData object) throws IOException {
        object.data(reader.readBytes(reader.readInt()));
    }
}
