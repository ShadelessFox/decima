package com.shade.decima.game.hfw.rtti.callbacks;

import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.ELanguage;
import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LocalizedTextResourceCallback implements ExtraBinaryDataCallback<LocalizedTextResourceCallback.TranslationData> {
    public interface TranslationData {
        @Attr(name = "Translations", type = "Array<String>", position = 0, offset = 0)
        List<String> translations();

        void translations(List<String> translations);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull TranslationData object) throws IOException {
        var count = ELanguage.values().length - 1; // Excluding "Unknown"
        var translations = new ArrayList<String>(count);
        for (int i = 0; i < count; i++) {
            translations.add(reader.readString(Short.toUnsignedInt(reader.readShort())));
        }
        object.translations(translations);
    }
}
