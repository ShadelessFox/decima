package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.RTTI;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class MorphemeAnimationResourceCallback implements ExtraBinaryDataCallback<MorphemeAnimationResourceCallback.MorphemeAnimationData> {
    public interface MorphemeAnimationData {
        @RTTI.Attr(name = "Data", type = "Array<uint8>", position = 0, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull MorphemeAnimationData object) throws IOException {
        object.data(reader.readBytes(reader.readInt()));
    }
}