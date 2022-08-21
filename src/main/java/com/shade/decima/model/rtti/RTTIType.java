package com.shade.decima.model.rtti;

import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public abstract class RTTIType<T_INSTANCE> {
    @NotNull
    public abstract T_INSTANCE read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer);

    public abstract void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull T_INSTANCE value);

    @NotNull
    public abstract Class<T_INSTANCE> getInstanceType();

    @NotNull
    public String getTypeName() {
        return toString();
    }
}
