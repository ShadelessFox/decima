package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeHashable;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.hash.CRC32C;
import com.shade.util.NotNull;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.function.Function;

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

    @NotNull
    private Hasher getHasher() {
        return getHasher(type, Function.identity());
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static Hasher getHasher(@NotNull RTTIType<?> type, @NotNull Function<Object, Object> extractor) {
        if (type instanceof RTTITypeHashable<?> hashable) {
            return obj -> ((RTTITypeHashable<Object>) hashable).getHash(extractor.apply(obj)) | 0x80000000;
        } else if (type.getTypeName().matches("\\w+_\\w+")) {
            return getHasher(((RTTIClass) type).getField("Key").getType(), obj -> ((RTTIObject) extractor.apply(obj)).get("Key"));
        } else if (type.getTypeName().equals("GGUUID")) {
            return obj -> {
                final RTTIObject object = (RTTIObject) extractor.apply(obj);
                final byte[] data = new byte[16];
                for (int i = 0; i < 16; i++) {
                    data[i] = object.i8("Data" + i);
                }
                return CRC32C.calculate(data) | 0x80000000;
            };
        } else {
            throw new IllegalArgumentException("Unable to get a hasher for type " + type);
        }
    }

    private interface Hasher {
        int hash(@NotNull Object value);
    }
}
