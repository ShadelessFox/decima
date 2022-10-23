package com.shade.decima.model.rtti;

import com.shade.util.NotNull;

public abstract class RTTITypeParameterized<T_INSTANCE, T_COMPONENT> extends RTTIType<T_INSTANCE> {
    @NotNull
    public abstract RTTIType<T_COMPONENT> getComponentType();
}
