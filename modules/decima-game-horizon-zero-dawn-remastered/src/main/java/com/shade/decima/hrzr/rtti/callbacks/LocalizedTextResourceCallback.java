package com.shade.decima.hrzr.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.shade.decima.rtti.HorizonZeroDawnRemastered.ELanguage;

public class LocalizedTextResourceCallback implements ExtraBinaryDataCallback<LocalizedTextResourceCallback.LocalizedTextData> {
    public interface LocalizedTextData {
        @Attr(name = "Texts", type = "Array<string>", position = 0, offset = 0)
        List<String> texts();

        void texts(List<String> value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull LocalizedTextData object) throws IOException {
        List<String> texts = new ArrayList<>(27);

        for (ELanguage value : ELanguage.values()) {
            if (value != ELanguage._1) {
                texts.add(reader.readString(reader.readShort()));
            }
        }

        object.texts(List.copyOf(texts));
    }
}
