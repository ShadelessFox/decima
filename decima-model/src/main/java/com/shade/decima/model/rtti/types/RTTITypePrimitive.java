package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.util.NotNull;

public abstract class RTTITypePrimitive<T_INSTANCE> extends RTTIType<T_INSTANCE> {
    @NotNull
    public abstract RTTITypePrimitive<? super T_INSTANCE> clone(@NotNull String name);
}
