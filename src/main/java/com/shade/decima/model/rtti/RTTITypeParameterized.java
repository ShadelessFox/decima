package com.shade.decima.model.rtti;

import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.NotNull;

public abstract class RTTITypeParameterized<T_INSTANCE, T_ARGUMENT> extends RTTIType<T_INSTANCE> {
    @NotNull
    public abstract RTTIType<T_ARGUMENT> getArgumentType();

    @NotNull
    @Override
    public String getTypeName() {
        return RTTITypeRegistry.getFullTypeName(this);
    }
}
