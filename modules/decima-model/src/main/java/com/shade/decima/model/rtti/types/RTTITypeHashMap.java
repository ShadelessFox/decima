package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeHashable;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotNull;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;

@RTTIDefinition({"HashMap", "HashSet"})
public class RTTITypeHashMap extends RTTITypeArray<Object> {
    public RTTITypeHashMap(@NotNull String name, @NotNull RTTIType<Object> type) {
        super(name, type);
    }

    @NotNull
    @Override
    public Object read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final Hasher hasher = getHasher();
        final int length = buffer.getInt();
        final Object array = Array.newInstance(type.getInstanceType(), length);

        for (int i = 0; i < length; i++) {
            final int checksum = buffer.getInt();
            final Object value = type.read(registry, buffer);

            if (hasher.hash(value) != checksum) {
                throw new IllegalArgumentException("Data is corrupted (mismatched checksum)");
            }

            set(array, i, value);
        }

        return array;
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull Object array) {
        final Hasher hasher = getHasher();
        final int length = length(array);

        buffer.putInt(length);

        for (int i = 0; i < length; i++) {
            final Object value = get(array, i);

            buffer.putInt(hasher.hash(value));
            type.write(registry, buffer, value);
        }
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull Object array) {
        int size = Integer.BYTES;

        for (int i = 0, length = length(array); i < length; i++) {
            size += type.getSize(registry, get(array, i));
            size += Integer.BYTES;
        }

        return size;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private Hasher getHasher() {
        if ((RTTIType<?>) type instanceof RTTIClass clazz) {
            final var field = clazz.getField("Key");
            final var type = field.getType();
            return obj -> ((RTTITypeHashable<Object>) type).getHash(field.get((RTTIObject) obj)) | 0x80000000;
        } else {
            return obj -> ((RTTITypeHashable<Object>) type).getHash(obj) | 0x80000000;
        }
    }

    private interface Hasher {
        int hash(@NotNull Object value);
    }
}
