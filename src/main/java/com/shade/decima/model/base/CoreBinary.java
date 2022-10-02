package com.shade.decima.model.base;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.java.RTTIExtends;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.decima.ui.data.registry.Type;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

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
            final long hash = buffer.getLong();
            final int size = buffer.getInt();
            final ByteBuffer slice = buffer.slice(buffer.position(), size).order(ByteOrder.LITTLE_ENDIAN);

            RTTIClass type;
            RTTIObject entry;

            try {
                type = (RTTIClass) registry.find(hash);
                entry = type.read(registry, slice);
            } catch (Exception e) {
                if (!lenient) {
                    throw e;
                }

                entry = null;
            }

            if (entry == null || slice.remaining() > 0) {
                entry = UnknownEntry.read(registry, slice.position(0), hash);
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

    @Nullable
    public RTTIObject find(@NotNull RTTIObject uuid) {
        for (RTTIObject entry : entries) {
            if (entry.getType().isInstanceOf("RTTIRefObject") && entry.get("ObjectUUID").equals(uuid)) {
                return entry;
            }
        }

        return null;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @RTTIExtends(@Type(name = "RTTIRefObject"))
    public static class UnknownEntry {
        @RTTIField(type = @Type(name = "GGUUID"), name = "ObjectUUID")
        public Object uuid;
        @RTTIField(type = @Type(name = "uint64"))
        public long hash;
        @RTTIField(type = @Type(name = "Array<uint8>"))
        public byte[] data;

        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, long hash) {
            final UnknownEntry entry = new UnknownEntry();
            entry.uuid = registry.find("GGUUID").read(registry, buffer);
            entry.hash = hash;
            entry.data = IOUtils.getBytesExact(buffer, buffer.remaining());

            return new RTTIObject(registry.find(UnknownEntry.class), entry);
        }
    }
}
