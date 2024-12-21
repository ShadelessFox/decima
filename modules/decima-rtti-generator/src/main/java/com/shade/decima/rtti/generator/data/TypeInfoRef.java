package com.shade.decima.rtti.generator.data;

import com.shade.decima.rtti.factory.TypeName;
import com.shade.util.NotNull;

public interface TypeInfoRef {
    @NotNull
    TypeName typeName();

    @NotNull
    TypeInfo value();
}
