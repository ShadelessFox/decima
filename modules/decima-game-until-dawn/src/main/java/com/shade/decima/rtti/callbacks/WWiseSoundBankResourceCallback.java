package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.RTTI;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
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
        static SoundBankData read(@NotNull BinaryReader reader) throws IOException {
            var object = RTTI.newInstance(SoundBankData.class);
            object.name(reader.readString(reader.readInt()));
            object.data(reader.readBytes(reader.readInt()));

            return object;
        }
    }

    public interface SoundBankList {
        @RTTI.Attr(name = "SoundBanks", type = "Array<SoundBankData>", position = 0, offset = 0)
        List<SoundBankData> soundBanks();

        void soundBanks(List<SoundBankData> value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull SoundBankList object) throws IOException {
        object.soundBanks(reader.readObjects(reader.readInt(), SoundBankData::read));
    }
}
