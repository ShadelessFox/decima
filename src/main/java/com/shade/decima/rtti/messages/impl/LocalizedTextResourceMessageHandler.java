package com.shade.decima.rtti.messages.impl;

import com.shade.decima.rtti.messages.RTTIMessageHandler;
import com.shade.decima.rtti.messages.RTTIMessageReadBinary;
import com.shade.decima.rtti.objects.RTTIObject;
import com.shade.decima.rtti.registry.RTTITypeRegistry;
import com.shade.decima.rtti.types.RTTITypeArray;
import com.shade.decima.rtti.types.RTTITypeClass;
import com.shade.decima.util.IOUtils;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;

@RTTIMessageHandler(type = "LocalizedTextResource", message = "MsgReadBinary")
public class LocalizedTextResourceMessageHandler implements RTTIMessageReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        final RTTITypeClass type = createVirtualClass("LocalizedTextResourceEntry", 3);
        type.getMembers()[0] = createVirtualMember(registry, type, "Text", "String");
        type.getMembers()[1] = createVirtualMember(registry, type, "Notes", "String");
        type.getMembers()[2] = createVirtualMember(registry, type, "Flags", "uint8");

        final RTTIObject[] entries = new RTTIObject[25];

        for (int i = 0; i < entries.length; i++) {
            final RTTIObject entry = new RTTIObject(type, new LinkedHashMap<>());
            entry.getMembers().put(type.getMembers()[0], new String(IOUtils.getBytesExact(buffer, buffer.getShort()), StandardCharsets.UTF_8));
            entry.getMembers().put(type.getMembers()[1], new String(IOUtils.getBytesExact(buffer, buffer.getShort()), StandardCharsets.UTF_8));
            entry.getMembers().put(type.getMembers()[2], buffer.get());
            entries[i] = entry;
        }

        object.getMembers().put(
            new RTTITypeClass.Member(object.getType(), new RTTITypeArray<>("Array", type), "Entries", "", 0, 0),
            entries
        );
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        throw new IllegalStateException("Not implemented");
    }

    @NotNull
    private static RTTITypeClass.Member createVirtualMember(@NotNull RTTITypeRegistry registry, @NotNull RTTITypeClass parent, @NotNull String name, @NotNull String type) {
        return new RTTITypeClass.Member(parent, registry.find(type), name, "", 0, 0);
    }

    @NotNull
    private static RTTITypeClass createVirtualClass(@NotNull String name, int members) {
        return new RTTITypeClass(name, new RTTITypeClass.Base[0], new RTTITypeClass.Member[members], Collections.emptyMap(), 0, 0);
    }
}
