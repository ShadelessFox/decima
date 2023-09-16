package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.RTTIEnum;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.hash.CRC32C;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

@RTTIDefinition({"HashMap", "HashSet"})
public class RTTITypeHashMap extends RTTITypeArray<RTTIObject> {
    public RTTITypeHashMap(@NotNull String name, @NotNull RTTIType<RTTIObject> type) {
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
            final RTTIObject value = type.read(registry, buffer);
            final int expected = hasher.hash(value) | 0x80000000;

            if (expected != checksum) {
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
            final RTTIObject value = get(array, i);

            buffer.putInt(hasher.hash(value) | 0x80000000);
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
        final RTTIType<?> key = ((RTTIClass) type).getField("Key").getType();

        if (key instanceof RTTITypeString) {
            return obj -> CRC32C.calculate(obj.str("Key").getBytes(StandardCharsets.UTF_8));
        } else if (key instanceof RTTITypeNumber<?> n) {
            return switch (n.getSize()) {
                case 1 -> obj -> CRC32C.calculate(IOUtils.toBytes(obj.i8("Key")));
                case 2 -> obj -> CRC32C.calculate(IOUtils.toBytes(obj.i16("Key"), ByteOrder.LITTLE_ENDIAN));
                case 4 -> obj -> CRC32C.calculate(IOUtils.toBytes(obj.i32("Key"), ByteOrder.LITTLE_ENDIAN));
                case 8 -> obj -> CRC32C.calculate(IOUtils.toBytes(obj.i64("Key"), ByteOrder.LITTLE_ENDIAN));
                default -> throw new IllegalArgumentException("Unexpected size: " + n.getSize());
            };
        } else if (key instanceof RTTIEnum e) {
            return obj -> CRC32C.calculate(IOUtils.toBytes(e.valueOf(obj.str("Key")).value(), ByteOrder.BIG_ENDIAN));
        } else {
            throw new IllegalArgumentException("Unsupported key type: " + key);
        }
    }

    private interface Hasher {
        int hash(@NotNull RTTIObject value);
    }
}
