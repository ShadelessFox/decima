package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.java.JavaObject;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.decima.ui.data.registry.Type;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(type = "LocalizedTextResource", message = "MsgReadBinary", game = GameType.DS)
public class LocalizedTextResourceMessageHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        final RTTIObject[] entries = new RTTIObject[25];

        for (int i = 0; i < entries.length; i++) {
            entries[i] = LanguageEntry.read(registry, buffer);
        }

        object.set("Entries", entries);
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[] {
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
        public static JavaObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final var object = new LanguageEntry();
            object.text = IOUtils.getString(buffer, buffer.getShort());
            object.notes = IOUtils.getString(buffer, buffer.getShort());
            object.flags = buffer.get();

            return new JavaObject(registry.find(LanguageEntry.class), object);
        }
    }
}
