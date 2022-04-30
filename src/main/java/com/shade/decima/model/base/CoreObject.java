package com.shade.decima.model.base;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class CoreObject {
    private final List<RTTIObject> entries;

    public CoreObject(@NotNull List<RTTIObject> entries) {
        this.entries = entries;
    }

    @NotNull
    public static CoreObject from(@NotNull byte[] data, @NotNull RTTITypeRegistry registry) {
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

        return new CoreObject(entries);
    }

    @NotNull
    public List<RTTIObject> getEntries() {
        return entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Nullable
    public RTTIObject getEntry(@NotNull RTTIObject uuid) {
        for (RTTIObject object : entries) {
            if (uuid.equals(object.get("ObjectUUID"))) {
                return object;
            }
        }
        return null;
    }
}
