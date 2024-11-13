package com.shade.decima.rtti.runtime;

import com.shade.decima.rtti.TypeName;
import com.shade.util.NotNull;

import java.lang.reflect.Type;

public sealed interface TypeInfo
    permits AtomTypeInfo, ClassTypeInfo, ContainerTypeInfo, EnumTypeInfo, PointerTypeInfo {

    @NotNull
    TypeName name();

    @NotNull
    Type type();
}
