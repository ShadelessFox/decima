package com.shade.decima.rtti.data;

import com.shade.util.NotNull;

public sealed interface TypeInfo
    permits AtomTypeInfo, ClassTypeInfo, EnumTypeInfo, ContainerTypeInfo, PointerTypeInfo {

    @NotNull
    String typeName();
}
