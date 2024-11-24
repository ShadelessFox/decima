package com.shade.decima.rtti.generator.data;

import com.shade.decima.rtti.factory.TypeName;
import com.shade.util.NotNull;

public sealed interface TypeInfo
    permits AtomTypeInfo, ClassTypeInfo, EnumTypeInfo, ContainerTypeInfo, PointerTypeInfo {

    @NotNull
    TypeName typeName();
}
