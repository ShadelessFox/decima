package com.shade.decima.rtti.runtime;

import com.shade.decima.rtti.factory.TypeName;
import com.shade.util.NotNull;

public interface TypeInfoRef {
    @NotNull
    TypeName name();

    @NotNull
    TypeInfo get();
}
