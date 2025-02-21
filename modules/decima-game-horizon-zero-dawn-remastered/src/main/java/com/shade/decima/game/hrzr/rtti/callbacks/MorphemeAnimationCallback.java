package com.shade.decima.game.hrzr.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class MorphemeAnimationCallback implements ExtraBinaryDataCallback<MorphemeAnimationCallback.MorphemeAnimationData> {
    public interface MorphemeAnimationData {
        @Attr(name = "Data", type = "Array<uint8>", position = 0, offset = 0)
        byte[] data();

        void data(byte[] value);

        @Attr(name = "Hash", type = "Array<uint8>", position = 1, offset = 0)
        byte[] hash();

        void hash(byte[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull MorphemeAnimationData object) throws IOException {
        object.data(reader.readBytes(reader.readInt()));
        object.hash(reader.readBytes(16));
    }
}
