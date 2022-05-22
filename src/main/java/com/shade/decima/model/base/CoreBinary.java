package com.shade.decima.model.base;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public record CoreBinary(@NotNull List<RTTIObject> entries) {
    @NotNull
    public static CoreBinary from(@NotNull byte[] data, @NotNull RTTITypeRegistry registry) {
        final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        final List<RTTIObject> entries = new ArrayList<>();

        while (buffer.remaining() > 0) {
            final long id = buffer.getLong();
            final int size = buffer.getInt();

            final RTTIType<?> type = registry.find(id);
            final RTTIObject entry = (RTTIObject) type.read(registry, buffer.slice(buffer.position(), size).order(ByteOrder.LITTLE_ENDIAN));

            buffer.position(buffer.position() + size);
            entries.add(entry);
        }

        return new CoreBinary(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
