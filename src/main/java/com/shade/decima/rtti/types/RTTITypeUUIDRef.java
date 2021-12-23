package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIGenericType;
import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;

public class RTTITypeUUIDRef<T> implements RTTIGenericType<RTTIReference, T> {
    private final RTTIType<T> type;

    public RTTITypeUUIDRef(@NotNull RTTIType<T> type) {
        this.type = type;
    }

    @NotNull
    @Override
    public RTTIReference read(@NotNull ByteBuffer buffer) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull RTTIReference value) {
        throw new IllegalStateException("Not implemented");
    }

    @NotNull
    @Override
    public String getName() {
        return "UUIDRef";
    }

    @NotNull
    @Override
    public Class<RTTIReference> getType() {
        return RTTIReference.class;
    }

    @Override
    public int getSize() {
        throw new IllegalStateException("getSize() is not implemented for dynamic containers");
    }

    @NotNull
    @Override
    public RTTIType<T> getUnderlyingType() {
        return type;
    }
}
