package com.shade.decima.rtti.generator.data;

import com.shade.util.NotNull;

public record ClassBaseInfo(
    @NotNull TypeInfoRef typeRef,
    int offset
) {
    @NotNull
    public ClassTypeInfo type() {
        return (ClassTypeInfo) typeRef.value();
    }
}
