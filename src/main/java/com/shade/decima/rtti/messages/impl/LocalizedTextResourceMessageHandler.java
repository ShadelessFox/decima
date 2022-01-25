package com.shade.decima.rtti.messages.impl;

import com.shade.decima.base.GameType;
import com.shade.decima.rtti.messages.RTTIMessageHandler;
import com.shade.decima.rtti.messages.RTTIMessageReadBinary;
import com.shade.decima.rtti.objects.RTTICollection;
import com.shade.decima.rtti.objects.RTTIObject;
import com.shade.decima.rtti.registry.RTTITypeRegistry;
import com.shade.decima.rtti.types.RTTITypeClass;
import com.shade.decima.util.IOUtils;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.RTTIUtils;

import java.nio.ByteBuffer;

@RTTIMessageHandler(type = "LocalizedTextResource", message = "MsgReadBinary", game = GameType.DS)
public class LocalizedTextResourceMessageHandler implements RTTIMessageReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        final RTTITypeClass type = RTTIUtils.newClassBuilder(registry, "LocalizedTextResourceEntry")
            .member("Text", "String")
            .member("Notes", "String")
            .member("Flags", "uint8")
            .build();

        final RTTIObject[] entries = new RTTIObject[25];

        for (int i = 0; i < entries.length; i++) {
            final RTTIObject entry = type.instantiate();
            entry.set("Text", IOUtils.getString(buffer, buffer.getShort()));
            entry.set("Notes", IOUtils.getString(buffer, buffer.getShort()));
            entry.set("Flags", buffer.get());
            entries[i] = entry;
        }

        object.set("Entries", new RTTICollection<>(type, entries), true);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        throw new IllegalStateException("Not implemented");
    }
}
