package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.NotNull;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;

public class RTTITypeArray<T> implements RTTIType<T[]> {
    private final RTTIType<T> type;

    public RTTITypeArray(@NotNull RTTIType<T> type) {
        this.type = type;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public T[] read(@NotNull ByteBuffer buffer) {
        final T[] items = (T[]) Array.newInstance(type.getType(), buffer.getInt());
        for (int i = 0; i < items.length; i++) {
            items[i] = type.read(buffer);
        }
        return items;
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull T[] values) {
        buffer.putInt(values.length);
        for (T value : values) {
            type.write(buffer, value);
        }
    }

    @NotNull
    @Override
    public String getName() {
        return "Array<" + type.getName() + ">";
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public Class<T[]> getType() {
        return (Class<T[]>) type.getType().arrayType();
    }

    @Override
    public int getSize() {
        throw new IllegalStateException("getSize() is not implemented for dynamic containers");
    }
}
