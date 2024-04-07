package com.shade.decima.model.rtti;

import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public abstract class RTTIType<T_INSTANCE> {
    @NotNull
    public abstract T_INSTANCE instantiate();

    @NotNull
    public abstract T_INSTANCE copyOf(@NotNull T_INSTANCE instance);

    @NotNull
    public abstract T_INSTANCE read(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer);

    public abstract void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer, @NotNull T_INSTANCE value);

    public abstract int getSize(@NotNull RTTIFactory factory, @NotNull T_INSTANCE value);

    public int getSize() {
        throw new UnsupportedOperationException("Can't determine the size of " + getFullTypeName() + " statically");
    }

    @NotNull
    public abstract Class<T_INSTANCE> getInstanceType();

    @NotNull
    public abstract String getTypeName();

    @NotNull
    public String getFullTypeName() {
        return getTypeName();
    }

    @Override
    public String toString() {
        return getFullTypeName();
    }
}
