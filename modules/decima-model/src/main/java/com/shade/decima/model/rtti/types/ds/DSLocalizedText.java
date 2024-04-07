package com.shade.decima.model.rtti.types.ds;

import com.shade.decima.model.rtti.RTTIEnum;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.java.HwLocalizedText;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class DSLocalizedText implements HwLocalizedText {
    @RTTIField(type = @Type(type = Entry[].class))
    public RTTIObject[] entries;

    @NotNull
    public static RTTIObject read(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        final RTTITypeEnum languages = factory.find("ELanguage");
        final RTTIObject[] entries = new RTTIObject[languages.values().length - 1];

        for (int i = 0; i < entries.length; i++) {
            entries[i] = Entry.read(factory, buffer, languages.valueOf(i + 1));
        }

        final var object = new DSLocalizedText();
        object.entries = entries;

        return new RTTIObject(factory.find(DSLocalizedText.class), object);
    }

    @Override
    public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        for (RTTIObject entry : entries) {
            entry.<Entry>cast().write(factory, buffer);
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
    public String getLanguage(int index) {
        return getEntry(index).language.name();
    }

    @NotNull
    @Override
    public String getTranslation(int index) {
        return getEntry(index).text;
    }

    @Override
    public void setTranslation(int index, @NotNull String translation) {
        getEntry(index).text = translation;
    }

    @NotNull
    @Override
    public DisplayMode getDisplayMode(int index) {
        return switch (getEntry(index).mode) {
            case 0 -> DisplayMode.SHOW_IF_SUBTITLES_ENABLED;
            case 1 -> DisplayMode.SHOW_ALWAYS;
            case 2 -> DisplayMode.SHOW_NEVER;
            default -> throw new IllegalStateException("Unexpected value: " + getEntry(index).mode);
        };
    }

    @Override
    public void setDisplayMode(int index, @NotNull DisplayMode mode) {
        getEntry(index).mode = switch (mode) {
            case SHOW_IF_SUBTITLES_ENABLED -> 0;
            case SHOW_ALWAYS -> 1;
            case SHOW_NEVER -> 2;
        };
    }

    @NotNull
    private Entry getEntry(int index) {
        return entries[index].cast();
    }

    public static class Entry {
        @RTTIField(type = @Type(name = "String"))
        public String text;
        @RTTIField(type = @Type(name = "String"))
        public String notes;
        @RTTIField(type = @Type(name = "uint8"))
        public byte mode;
        @RTTIField(type = @Type(name = "ELanguage"))
        public RTTIEnum.Constant language;

        @NotNull
        public static RTTIObject read(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer, @NotNull RTTIEnum.Constant language) {
            final var entry = new Entry();
            entry.text = BufferUtils.getString(buffer, buffer.getShort());
            entry.notes = BufferUtils.getString(buffer, buffer.getShort());
            entry.mode = buffer.get();
            entry.language = language;

            return new RTTIObject(factory.find(Entry.class), entry);
        }

        public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
            final byte[] text = this.text.getBytes(StandardCharsets.UTF_8);
            final byte[] notes = this.notes.getBytes(StandardCharsets.UTF_8);

            buffer.putShort((short) text.length);
            buffer.put(text);
            buffer.putShort((short) notes.length);
            buffer.put(notes);
            buffer.put(mode);
        }

        public int getSize() {
            final byte[] text = this.text.getBytes(StandardCharsets.UTF_8);
            final byte[] notes = this.notes.getBytes(StandardCharsets.UTF_8);

            return 5 + text.length + notes.length;
        }
    }
}
