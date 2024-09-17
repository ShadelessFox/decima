package com.shade.decima.rtti.generator.data;

import com.shade.util.NotNull;

public interface TypeInfoRef {
    @NotNull
    String typeName();

    @NotNull
    TypeInfo value();
}
