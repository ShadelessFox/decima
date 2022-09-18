package com.shade.decima.ui.data;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.platform.model.LazyWithMetadata;
import com.shade.platform.model.util.ReflectionUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.List;

public class ValueManagerRegistry {
    private static final ValueManagerRegistry INSTANCE = new ValueManagerRegistry();

    @SuppressWarnings("rawtypes")
    private final List<LazyWithMetadata<ValueManager, ValueManagerRegistration>> managers;

    private ValueManagerRegistry() {
        this.managers = ReflectionUtils.findAnnotatedTypes(ValueManager.class, ValueManagerRegistration.class);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> ValueManager<T> findManager(@NotNull RTTIType<T> type) {
        for (var manager : INSTANCE.managers) {
            for (String typeName : manager.metadata().names()) {
                if (type.getTypeName().equals(typeName)) {
                    return (ValueManager<T>) manager.get();
                }

                if (type instanceof RTTITypeClass c && c.isInstanceOf(typeName)) {
                    return (ValueManager<T>) manager.get();
                }
            }

            for (Class<?> typeClass : manager.metadata().types()) {
                if (typeClass.isInstance(type)) {
                    return (ValueManager<T>) manager.get();
                }
            }
        }

        return null;
    }
}
