package com.shade.decima.model.rtti.types.hzd;

import com.shade.decima.model.rtti.RTTIEnum;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.java.HwLocalizedText;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class HZDLocalizedText implements HwLocalizedText {
    @RTTIField(type = @Type(type = Entry[].class))
    public RTTIObject[] entries;

    @NotNull
    public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final RTTITypeEnum languages = registry.find("ELanguage");
        final RTTIObject[] entries = new RTTIObject[languages.getConstants().length - 1];

        for (int i = 0; i < entries.length; i++) {
            entries[i] = Entry.read(registry, buffer, languages.valueOf(i + 1));
        }

        final var object = new HZDLocalizedText();
        object.entries = entries;

        return new RTTIObject(registry.find(HZDLocalizedText.class), object);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        for (RTTIObject entry : entries) {
            entry.<Entry>cast().write(registry, buffer);
        }
    }

    @Override
    public int getSize() {
        return Arrays.stream(entries)
            .map(RTTIObject::<Entry>cast)
            .mapToInt(Entry::getSize)
            .sum();
    }

    @Override
    public int getLocalizationCount() {
        return entries.length;
    }

    @NotNull
    @Override
    public String getLocalizationLanguage(int index) {
        return entries[index].<Entry>cast().language.name();
    }

    @NotNull
    @Override
    public String getLocalizationText(int index) {
        return entries[index].<Entry>cast().text;
    }

    public static class Entry {
        @RTTIField(type = @Type(name = "String"))
        public String text;
        @RTTIField(type = @Type(name = "ELanguage"))
        public RTTIEnum.Constant language;

        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIEnum.Constant language) {
            final var object = new Entry();
            object.text = BufferUtils.getString(buffer, buffer.getShort());
            object.language = language;

            return new RTTIObject(registry.find(Entry.class), object);
        }

        public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final byte[] text = this.text.getBytes(StandardCharsets.UTF_8);

            buffer.putShort((short) text.length);
            buffer.put(text);
        }

        public int getSize() {
            return 2 + text.getBytes(StandardCharsets.UTF_8).length;
        }
    }
}
