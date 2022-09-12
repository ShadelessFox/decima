package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeContainer;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotNull;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;

@RTTIDefinition(name = "HashMap", aliases = {"HashSet"})
public class RTTITypeHashMap<T> extends RTTITypeContainer<T[], T> {
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
        final T[] values = (T[]) Array.newInstance(type.getInstanceType(), buffer.getInt());
        for (int i = 0; i < values.length; i++) {
            buffer.getInt();
            values[i] = type.read(registry, buffer);
        }
        return values;
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull T[] values) {
        throw new IllegalStateException("Not implemented");
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
    public RTTIType<T> getArgumentType() {
        return type;
    }
}
