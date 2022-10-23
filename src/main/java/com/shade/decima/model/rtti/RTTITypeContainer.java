package com.shade.decima.model.rtti;

import com.shade.util.NotNull;

public abstract class RTTITypeContainer<T_INSTANCE, T_COMPONENT> extends RTTITypeParameterized<T_INSTANCE, T_COMPONENT> {
    public abstract int length(@NotNull T_INSTANCE instance);
}
