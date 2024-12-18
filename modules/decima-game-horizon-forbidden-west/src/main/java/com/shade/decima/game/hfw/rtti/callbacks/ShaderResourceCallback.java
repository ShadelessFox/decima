package com.shade.decima.game.hfw.rtti.callbacks;

import com.shade.decima.game.hfw.rtti.HFWTypeReader;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.MurmurHashValue;
import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class ShaderResourceCallback implements ExtraBinaryDataCallback<ShaderResourceCallback.ShaderData> {
    public interface ShaderData {
        @Attr(name = "Hash", type = "MurmurHashValue", position = 0, offset = 0)
        MurmurHashValue hash();

        void hash(MurmurHashValue hash);

        @Attr(name = "Data", type = "uint8", position = 1, offset = 8)
        byte[] data();

        void data(byte[] data);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull ShaderData object) throws IOException {
        var size = reader.readInt();
        object.hash(HFWTypeReader.readCompound(MurmurHashValue.class, reader, factory));
        object.data(reader.readBytes(size));
    }
}
