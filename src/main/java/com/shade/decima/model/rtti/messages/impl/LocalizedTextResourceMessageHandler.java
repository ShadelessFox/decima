package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.decima.ui.data.registry.Type;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "LocalizedTextResource", game = GameType.DS),
    @Type(name = "LocalizedTextResource", game = GameType.DSDC)
})
public class LocalizedTextResourceMessageHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final RTTIObject[] entries = new RTTIObject[25];

        for (int i = 0; i < entries.length; i++) {
            entries[i] = LanguageEntry.read(registry, buffer);
        }

        object.set("Entries", entries);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        for (RTTIObject entry : object.objs("Entries")) {
            entry.<LanguageEntry>cast().write(buffer);
        }
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object) {
        return Arrays.stream(object.objs("Entries"))
            .map(RTTIObject::<LanguageEntry>cast)
            .mapToInt(LanguageEntry::getSize)
            .sum();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[]{
            new Component("Entries", registry.find(LanguageEntry[].class))
        };
    }

    public static class LanguageEntry {
        @RTTIField(type = @Type(name = "String"))
        public String text;
        @RTTIField(type = @Type(name = "String"))
        public String notes;
        @RTTIField(type = @Type(name = "uint8"))
        public byte flags;

        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final var object = new LanguageEntry();
            object.text = IOUtils.getString(buffer, buffer.getShort());
            object.notes = IOUtils.getString(buffer, buffer.getShort());
            object.flags = buffer.get();

            return new RTTIObject(registry.find(LanguageEntry.class), object);
        }

        public void write(@NotNull ByteBuffer buffer) {
            final byte[] text = this.text.getBytes(StandardCharsets.UTF_8);
            final byte[] notes = this.notes.getBytes(StandardCharsets.UTF_8);

            buffer.putShort((short) text.length);
            buffer.put(text);
            buffer.putShort((short) notes.length);
            buffer.put(notes);
            buffer.put(flags);
        }

        public int getSize() {
            final byte[] text = this.text.getBytes(StandardCharsets.UTF_8);
            final byte[] notes = this.notes.getBytes(StandardCharsets.UTF_8);

            return 5 + text.length + notes.length;
        }
    }
}
