package com.shade.decima.game.until_dawn.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.List;

public class WWiseSoundBankResourceCallback implements ExtraBinaryDataCallback<WWiseSoundBankResourceCallback.SoundBankList> {
    public interface SoundBankData {
        @Attr(name = "Name", type = "String", position = 0, offset = 0)
        String name();

        void name(String value);

        @Attr(name = "Data", type = "Array<uint8>", position = 1, offset = 0)
        byte[] data();

        void data(byte[] value);

        @NotNull
        static SoundBankData read(@NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
            var object = factory.newInstance(SoundBankData.class);
            object.name(reader.readString(reader.readInt()));
            object.data(reader.readBytes(reader.readInt()));

            return object;
        }
    }

    public interface SoundBankList {
        @Attr(name = "SoundBanks", type = "Array<SoundBankData>", position = 0, offset = 0)
        List<SoundBankData> soundBanks();

        void soundBanks(List<SoundBankData> value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull SoundBankList object) throws IOException {
        object.soundBanks(reader.readObjects(reader.readInt(), r -> SoundBankData.read(r, factory)));
    }
}
