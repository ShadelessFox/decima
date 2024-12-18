package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.RTTITypeHashable;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.platform.model.util.BufferUtils;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.hash.Hashing;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

@RTTIDefinition({
    "int", "int8", "int16", "int32", "int64",
    "uint", "uint8", "uint16", "uint32", "uint64", "uint128",
    "float", "double", "HalfFloat",
    "wchar", "ucs4"
})
public final class RTTITypeNumber<T extends Number> extends RTTITypePrimitive<T> implements RTTITypeHashable<T> {
    private static final Map<String, Descriptor<?>> DESCRIPTORS = Map.ofEntries(
        Map.entry("int8", new Descriptor<>(byte.class, ByteBuffer::get, ByteBuffer::put, (byte) 0, Byte.BYTES, true)),
        Map.entry("uint8", new Descriptor<>(byte.class, ByteBuffer::get, ByteBuffer::put, (byte) 0, Byte.BYTES, false)),
        Map.entry("int16", new Descriptor<>(short.class, ByteBuffer::getShort, ByteBuffer::putShort, (short) 0, Short.BYTES, true)),
        Map.entry("uint16", new Descriptor<>(short.class, ByteBuffer::getShort, ByteBuffer::putShort, (short) 0, Short.BYTES, false)),
        Map.entry("int", new Descriptor<>(int.class, ByteBuffer::getInt, ByteBuffer::putInt, 0, Integer.BYTES, true)),
        Map.entry("uint", new Descriptor<>(int.class, ByteBuffer::getInt, ByteBuffer::putInt, 0, Integer.BYTES, false)),
        Map.entry("int32", new Descriptor<>(int.class, ByteBuffer::getInt, ByteBuffer::putInt, 0, Integer.BYTES, true)),
        Map.entry("uint32", new Descriptor<>(int.class, ByteBuffer::getInt, ByteBuffer::putInt, 0, Integer.BYTES, false)),
        Map.entry("int64", new Descriptor<>(long.class, ByteBuffer::getLong, ByteBuffer::putLong, 0L, Long.BYTES, true)),
        Map.entry("uint64", new Descriptor<>(long.class, ByteBuffer::getLong, ByteBuffer::putLong, 0L, Long.BYTES, false)),
        Map.entry("uint128", new Descriptor<>(BigInteger.class, BufferUtils::getUInt128, BufferUtils::putUInt128, BigInteger.ZERO, Long.BYTES * 2, false)),
        Map.entry("float", new Descriptor<>(float.class, ByteBuffer::getFloat, ByteBuffer::putFloat, 0f, Float.BYTES, true)),
        Map.entry("double", new Descriptor<>(double.class, ByteBuffer::getDouble, ByteBuffer::putDouble, 0d, Double.BYTES, true)),
        Map.entry("HalfFloat", new Descriptor<>(float.class, BufferUtils::getHalfFloat, BufferUtils::putHalfFloat, 0f, Short.BYTES, true)),
        Map.entry("wchar", new Descriptor<>(short.class, ByteBuffer::getShort, ByteBuffer::putShort, (short) 0, Short.BYTES, false)),
        Map.entry("ucs4", new Descriptor<>(int.class, ByteBuffer::getInt, ByteBuffer::putInt, 0, Integer.BYTES, false))
    );

    private final String name;
    private final Descriptor<T> descriptor;

    @SuppressWarnings({"unchecked", "unused"})
    public RTTITypeNumber(@NotNull String name) {
        this(name, (Descriptor<T>) Objects.requireNonNull(DESCRIPTORS.get(name), "Couldn't find descriptor for numeric type " + name));
    }

    private RTTITypeNumber(@NotNull String name, @NotNull Descriptor<T> descriptor) {
        this.name = name;
        this.descriptor = descriptor;
    }

    @NotNull
    @Override
    public T instantiate() {
        return descriptor.initialValue;
    }

    @NotNull
    @Override
    public T copyOf(@NotNull T value) {
        return value;
    }

    @NotNull
    @Override
    public T read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        return descriptor.reader.apply(buffer);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull T value) {
        descriptor.writer.accept(buffer, value);
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull T value) {
        return descriptor.size;
    }

    @Override
    public int getSize() {
        return descriptor.size;
    }

    @NotNull
    @Override
    public String getTypeName() {
        return name;
    }

    @NotNull
    @Override
    public Class<T> getInstanceType() {
        return descriptor.type;
    }

    @NotNull
    @Override
    public RTTITypePrimitive<? super T> clone(@NotNull String name) {
        return new RTTITypeNumber<>(name, descriptor);
    }

    @Override
    public int getHash(@NotNull T value) {
        final byte[] bytes = switch (getSize()) {
            case 1 -> IOUtils.toBytes((byte) value);
            case 2 -> IOUtils.toBytes((short) value, ByteOrder.LITTLE_ENDIAN);
            case 4 -> IOUtils.toBytes((int) value, ByteOrder.LITTLE_ENDIAN);
            case 8 -> IOUtils.toBytes((long) value, ByteOrder.LITTLE_ENDIAN);
            default -> throw new IllegalArgumentException("Unexpected size: " + getSize());
        };

        return Hashing.decimaCrc32().hashBytes(bytes).asInt();
    }

    public boolean read(@NotNull ByteBuffer buffer, @NotNull Object array, int offset, int length) {
        final Class<T> type = descriptor.type;
        final int position = buffer.position();

        if (type == byte.class) {
            buffer.get((byte[]) array, offset, length);
        } else if (type == short.class) {
            buffer.asShortBuffer().get((short[]) array, offset, length);
        } else if (type == int.class) {
            buffer.asIntBuffer().get((int[]) array, offset, length);
        } else if (type == long.class) {
            buffer.asLongBuffer().get((long[]) array, offset, length);
        } else if (type == float.class) {
            buffer.asFloatBuffer().get((float[]) array, offset, length);
        } else if (type == double.class) {
            buffer.asDoubleBuffer().get((double[]) array, offset, length);
        } else {
            return false;
        }

        buffer.position(position + length * descriptor.size);
        return true;
    }

    public boolean write(@NotNull ByteBuffer buffer, @NotNull Object array, int offset, int length) {
        final Class<T> type = descriptor.type;
        final int position = buffer.position();

        if (type == byte.class) {
            buffer.put((byte[]) array, offset, length);
        } else if (type == short.class) {
            buffer.asShortBuffer().put((short[]) array, offset, length);
        } else if (type == int.class) {
            buffer.asIntBuffer().put((int[]) array, offset, length);
        } else if (type == long.class) {
            buffer.asLongBuffer().put((long[]) array, offset, length);
        } else if (type == float.class) {
            buffer.asFloatBuffer().put((float[]) array, offset, length);
        } else if (type == double.class) {
            buffer.asDoubleBuffer().put((double[]) array, offset, length);
        } else {
            return false;
        }

        buffer.position(position + length * descriptor.size);
        return true;
    }

    public boolean isSigned() {
        return descriptor.signed;
    }

    public boolean isDecimal() {
        return descriptor.type == Float.class || descriptor.type == Double.class;
    }

    private record Descriptor<T extends Number>(
        @NotNull Class<T> type,
        @NotNull Function<ByteBuffer, T> reader,
        @NotNull BiConsumer<ByteBuffer, T> writer,
        @NotNull T initialValue,
        int size,
        boolean signed
    ) {}
}
