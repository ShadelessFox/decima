package com.shade.decima.model.rtti;

import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.NotNull;

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
