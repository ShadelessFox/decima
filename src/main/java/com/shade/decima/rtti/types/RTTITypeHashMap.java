package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIDefinition;
import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.RTTITypeContainer;
import com.shade.decima.util.NotNull;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;

@RTTIDefinition(name = "HashMap", aliases = {"HashSet"})
public class RTTITypeHashMap<T> extends RTTITypeContainer<T[]> {
    private final String name;
    private final RTTIType<T> type;

    public RTTITypeHashMap(@NotNull String name, @NotNull RTTIType<T> type) {
        this.name = name;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public T[] read(@NotNull ByteBuffer buffer) {
        final T[] values = (T[]) Array.newInstance(type.getComponentType(), buffer.getInt());
        for (int i = 0; i < values.length; i++) {
            buffer.getInt();
            values[i] = type.read(buffer);
        }
        return values;
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull T[] values) {
        throw new IllegalStateException("Not implemented");
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public Class<T[]> getComponentType() {
        return (Class<T[]>) type.getComponentType().arrayType();
    }

    @NotNull
    @Override
    public RTTIType<?> getContainedType() {
        return type;
    }
}
