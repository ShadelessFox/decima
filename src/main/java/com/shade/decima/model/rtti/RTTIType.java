package com.shade.decima.model.rtti;

import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public abstract class RTTIType<T_INSTANCE> {
    @NotNull
    public abstract T_INSTANCE instantiate();

    @NotNull
    public abstract T_INSTANCE read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer);

    public abstract void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull T_INSTANCE value);

    public abstract int getSize(@NotNull RTTITypeRegistry registry, @NotNull T_INSTANCE value);

    public int getSize() {
        throw new UnsupportedOperationException("Can't determine the size of " + RTTITypeRegistry.getFullTypeName(this) + " statically");
    }

    @NotNull
    public abstract Class<T_INSTANCE> getInstanceType();

    @NotNull
    public abstract String getTypeName();
}
