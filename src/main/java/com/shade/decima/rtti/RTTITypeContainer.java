package com.shade.decima.rtti;

import com.shade.decima.rtti.registry.RTTITypeRegistry;
import com.shade.decima.util.NotNull;

public abstract class RTTITypeContainer<T> implements RTTIType<T> {
    @NotNull
    public abstract RTTIType<?> getContainedType();

    @NotNull
    @Override
    public Kind getKind() {
        return Kind.CONTAINER;
    }

    @Override
    public String toString() {
        return RTTITypeRegistry.getFullTypeName(this);
    }
}
