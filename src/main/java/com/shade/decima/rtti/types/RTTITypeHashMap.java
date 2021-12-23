package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;

public class RTTITypeHashMap<T> extends RTTITypeArray<T> {
    public RTTITypeHashMap(@NotNull RTTIType<T> type) {
        super(type);
    }

    @NotNull
    @Override
    public T[] read(@NotNull ByteBuffer buffer) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull T[] values) {
        throw new IllegalStateException("Not implemented");
    }

    @NotNull
    @Override
    public String getName() {
        return "HashMap";
    }
}
