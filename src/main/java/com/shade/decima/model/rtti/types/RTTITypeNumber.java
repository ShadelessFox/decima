package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

@RTTIDefinition({
    "int", "int8", "int16", "int32", "int64",
    "uint", "uint8", "uint16", "uint32", "uint64", "uint128",
    "float", "double"
})
public class RTTITypeNumber<T extends Number> extends RTTITypePrimitive<T> {
    private static final Map<String, Descriptor<?>> DESCRIPTORS = Map.ofEntries(
        Map.entry("int8", new Descriptor<>(byte.class, ByteBuffer::get, ByteBuffer::put, Byte.BYTES, true)),
        Map.entry("uint8", new Descriptor<>(byte.class, ByteBuffer::get, ByteBuffer::put, Byte.BYTES, false)),
        Map.entry("int16", new Descriptor<>(short.class, ByteBuffer::getShort, ByteBuffer::putShort, Short.BYTES, true)),
        Map.entry("uint16", new Descriptor<>(short.class, ByteBuffer::getShort, ByteBuffer::putShort, Short.BYTES, false)),
        Map.entry("int", new Descriptor<>(int.class, ByteBuffer::getInt, ByteBuffer::putInt, Integer.BYTES, true)),
        Map.entry("uint", new Descriptor<>(int.class, ByteBuffer::getInt, ByteBuffer::putInt, Integer.BYTES, false)),
        Map.entry("int32", new Descriptor<>(int.class, ByteBuffer::getInt, ByteBuffer::putInt, Integer.BYTES, true)),
        Map.entry("uint32", new Descriptor<>(int.class, ByteBuffer::getInt, ByteBuffer::putInt, Integer.BYTES, false)),
        Map.entry("int64", new Descriptor<>(long.class, ByteBuffer::getLong, ByteBuffer::putLong, Long.BYTES, true)),
        Map.entry("uint64", new Descriptor<>(long.class, ByteBuffer::getLong, ByteBuffer::putLong, Long.BYTES, false)),
        Map.entry("uint128", new Descriptor<>(BigInteger.class, IOUtils::getUInt128, IOUtils::putUInt128, Long.BYTES * 2, false)),
        Map.entry("float", new Descriptor<>(float.class, ByteBuffer::getFloat, ByteBuffer::putFloat, Float.BYTES, true)),
        Map.entry("double", new Descriptor<>(double.class, ByteBuffer::getDouble, ByteBuffer::putDouble, Double.BYTES, true))
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

    public boolean isSigned() {
        return descriptor.signed;
    }

    public boolean isDecimal() {
        return descriptor.type == Float.class || descriptor.type == Double.class;
    }

    private record Descriptor<T extends Number>(@NotNull Class<T> type, @NotNull Function<ByteBuffer, T> reader, @NotNull BiConsumer<ByteBuffer, T> writer, int size, boolean signed) {}
}
