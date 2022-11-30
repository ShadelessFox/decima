package com.shade.decima.model.base;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public record CoreBinary(@NotNull List<RTTIObject> entries) {
    @NotNull
    public static CoreBinary from(@NotNull byte[] data, @NotNull RTTITypeRegistry registry) {
        return from(data, registry, false);
    }

    @NotNull
    public static CoreBinary from(@NotNull byte[] data, @NotNull RTTITypeRegistry registry, boolean lenient) {
        final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        final List<RTTIObject> entries = new ArrayList<>();

        while (buffer.remaining() > 0) {
            final long id = buffer.getLong();
            final int size = buffer.getInt();
            final ByteBuffer slice = buffer.slice(buffer.position(), size).order(ByteOrder.LITTLE_ENDIAN);

            RTTITypeClass type;
            RTTIObject entry;

            try {
                type = (RTTITypeClass) registry.find(id);
                entry = type.read(registry, slice);
            } catch (Exception e) {
                if (!lenient) {
                    throw e;
                }

                entry = null;
            }

            if (entry == null) {
                type = RTTIUtils.newClassBuilder(registry, "RTTIRefObject", "UnknownEntry<%8x>".formatted(id)).build();
                entry = type.read(registry, slice);
            }

            if (slice.remaining() > 0) {
                entry.define("$Remaining", registry.find("Array<uint8>"), IOUtils.getBytesExact(slice, slice.remaining()));
            }

            entries.add(entry);
            buffer.position(buffer.position() + size);
        }

        return new CoreBinary(entries);
    }

    @NotNull
    public byte[] serialize(@NotNull RTTITypeRegistry registry) {
        final int size = entries.stream().mapToInt(obj -> obj.type().getSize(registry, obj) + 12).sum();
        final var data = new byte[size];
        final var buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        for (RTTIObject entry : entries) {
            final RTTIClass type = entry.type();

            buffer.putLong(registry.getHash(type));
            buffer.putInt(type.getSize(registry, entry));
            type.write(registry, buffer, entry);
        }

        return data;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
