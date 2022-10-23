package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeContainer;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.hash.CRC32C;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@RTTIDefinition({"HashMap", "HashSet"})
public class RTTITypeHashMap<T extends RTTIObject> extends RTTITypeContainer<T[], T> {
    private final String name;
    private final RTTIType<T> type;

    public RTTITypeHashMap(@NotNull String name, @NotNull RTTIType<T> type) {
        this.name = name;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public T[] read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final Hasher hasher = getHasher();
        final T[] values = (T[]) Array.newInstance(type.getInstanceType(), buffer.getInt());

        for (int i = 0; i < values.length; i++) {
            final int checksum = buffer.getInt();
            final T value = type.read(registry, buffer);

            if (hasher.hash(value) != checksum) {
                throw new IllegalArgumentException("Data is corrupted (mismatched checksum)");
            }

            values[i] = value;
        }

        return values;
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull T[] values) {
        final Hasher hasher = getHasher();

        buffer.putInt(values.length);

        for (T value : values) {
            buffer.putInt(hasher.hash(value));
            type.write(registry, buffer, value);
        }
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull T[] values) {
        int size = Integer.BYTES;
        for (T value : values) {
            size += type.getSize(registry, value);
            size += Integer.BYTES;
        }
        return size;
    }

    @NotNull
    @Override
    public String getTypeName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public Class<T[]> getInstanceType() {
        return (Class<T[]>) type.getInstanceType().arrayType();
    }

    @NotNull
    @Override
    public RTTIType<T> getComponentType() {
        return type;
    }

    @NotNull
    private Hasher getHasher() {
        final RTTIType<?> key = ((RTTITypeClass) type).getMember("Key").type();

        if (key instanceof RTTITypeString) {
            return obj -> CRC32C.calculate(obj.str("Key").getBytes(StandardCharsets.UTF_8)) | 0x80000000;
        } else if (key instanceof RTTITypeNumber<?> n && n.getSize() == Integer.BYTES) {
            return obj -> CRC32C.calculate(IOUtils.toByteArray(obj.i32("Key"))) | 0x80000000;
        } else {
            throw new IllegalArgumentException("Unsupported key type: " + key);
        }
    }

    private interface Hasher {
        int hash(@NotNull RTTIObject value);
    }
}
