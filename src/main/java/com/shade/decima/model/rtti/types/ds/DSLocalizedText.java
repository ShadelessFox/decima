package com.shade.decima.model.rtti.types.ds;

import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.java.HwLocalizedText;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class DSLocalizedText implements HwLocalizedText {
    @RTTIField(type = @Type(name = "String"))
    public String text;
    @RTTIField(type = @Type(name = "String"))
    public String notes;
    @RTTIField(type = @Type(name = "uint8"))
    public byte flags;

    @NotNull
    public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final var object = new DSLocalizedText();
        object.text = IOUtils.getString(buffer, buffer.getShort());
        object.notes = IOUtils.getString(buffer, buffer.getShort());
        object.flags = buffer.get();

        return new RTTIObject(registry.find(DSLocalizedText.class), object);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final byte[] text = this.text.getBytes(StandardCharsets.UTF_8);
        final byte[] notes = this.notes.getBytes(StandardCharsets.UTF_8);

        buffer.putShort((short) text.length);
        buffer.put(text);
        buffer.putShort((short) notes.length);
        buffer.put(notes);
        buffer.put(flags);
    }

    @Override
    public int getSize() {
        final byte[] text = this.text.getBytes(StandardCharsets.UTF_8);
        final byte[] notes = this.notes.getBytes(StandardCharsets.UTF_8);

        return 5 + text.length + notes.length;
    }

    @NotNull
    @Override
    public String getText() {
        return text;
    }
}
