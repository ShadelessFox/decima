package com.shade.decima.rtti.runtime;

import com.shade.decima.rtti.factory.TypeName;
import com.shade.util.NotNull;

public sealed interface TypeInfo
    permits AtomTypeInfo, ClassTypeInfo, ContainerTypeInfo, EnumTypeInfo, PointerTypeInfo {

    @NotNull
    TypeName name();

    @NotNull
    Class<?> type();
}
