package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.RTTI;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.List;

public class WWiseSoundBankResourceCallback implements ExtraBinaryDataCallback<WWiseSoundBankResourceCallback.SoundBankList> {
    public interface SoundBankData {
        @RTTI.Attr(name = "Name", type = "String", position = 0, offset = 0)
        String name();

        void name(String value);

        @RTTI.Attr(name = "Data", type = "Array<uint8>", position = 1, offset = 0)
        byte[] data();

        void data(byte[] value);

        @NotNull
        static SoundBankData read(@NotNull ByteBuffer buffer) {
            var object = RTTI.newInstance(SoundBankData.class);
            object.name(BufferUtils.getString(buffer, buffer.getInt()));
            object.data(BufferUtils.getBytes(buffer, buffer.getInt()));

            return object;
        }
    }

    public interface SoundBankList {
        @RTTI.Attr(name = "SoundBanks", type = "Array<SoundBankData>", position = 0, offset = 0)
        List<SoundBankData> soundBanks();

        void soundBanks(List<SoundBankData> value);
    }

    @Override
    public void deserialize(@NotNull ByteBuffer buffer, @NotNull SoundBankList object) {
        object.soundBanks(BufferUtils.getStructs(buffer, buffer.getInt(), SoundBankData::read));
    }
}
