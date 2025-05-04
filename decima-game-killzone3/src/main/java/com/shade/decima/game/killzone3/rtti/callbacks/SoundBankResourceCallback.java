package com.shade.decima.game.killzone3.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class SoundBankResourceCallback implements ExtraBinaryDataCallback<SoundBankResourceCallback.SoundBankResourceData> {
    public interface SoundBankResourceData {
        @Attr(name = "Data", type = "Array<uint8>", position = 1, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(BinaryReader reader, TypeFactory factory, SoundBankResourceData object) throws IOException {
        object.data(reader.readBytes(reader.readInt()));
    }
}
