package com.shade.decima.rtti.runtime;

import com.shade.util.NotNull;

public interface TypedObject {
    @NotNull
    ClassTypeInfo getType();
}
