package com.shade.decima.rtti;

import com.shade.decima.rtti.types.*;
import com.shade.decima.util.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class RTTITypeRegistry {
    private static final Map<String, RTTIType<?>> types = new HashMap<>();

    static {
        register(new RTTITypeUInt8());
        register(new RTTITypeUInt16());
        register(new RTTITypeUInt32());
        register(new RTTITypeUInt64());

        register(new RTTITypeString());
        register(new RTTITypeUUID());
    }

    private RTTITypeRegistry() {
    }

    public static synchronized <T> void register(@NotNull RTTIType<T> type) {
        final String name = type.getName();
        if (types.containsKey(name)) {
            throw new IllegalArgumentException("Type with name '" + name + "' already present in the registry");
        }
        types.put(name, type);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T extends RTTIType<?>> T find(@NotNull String name) {
        final RTTIType<?> type = types.get(name);
        if (type == null) {
            throw new IllegalArgumentException("Type with name '" + name + "' is missing in the registry");
        }
        return (T) type;
    }
}
