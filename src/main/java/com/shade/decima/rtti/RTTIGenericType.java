package com.shade.decima.rtti;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.NotNull;

public interface RTTIGenericType<T, U> extends RTTIType<T> {
    @NotNull
    RTTIType<U> getUnderlyingType();
}
