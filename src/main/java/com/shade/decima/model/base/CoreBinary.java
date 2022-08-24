package com.shade.decima.model.base;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
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

            // It doesn't seem that this is the best place for handling the `lenient` option.
            // We need to figure a better way to display unknown or incomplete entries in UI.

            try {
                final RTTIType<?> type = registry.find(id);
                final RTTIObject entry = (RTTIObject) type.read(registry, slice);

                if (slice.remaining() > 0) {
                    final Byte[] remaining = IOUtils.boxed(IOUtils.getBytesExact(slice, slice.remaining()));
                    entry.define("$Remaining", registry.find("Array<uint8>"), remaining);
                }

                entries.add(entry);
            } catch (Exception e) {
                if (!lenient) {
                    throw e;
                }

                final Byte[] remaining = IOUtils.boxed(IOUtils.getBytesExact(slice, slice.position(0).remaining()));
                final RTTIObject entry = RTTIUtils.newClassBuilder(registry, "UnknownEntry<%8x>".formatted(id))
                    .member("Data", "Array<uint8>")
                    .build().instantiate();

                entry.set("Data", remaining);
                entries.add(entry);
            }

            buffer.position(buffer.position() + size);
        }

        return new CoreBinary(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
