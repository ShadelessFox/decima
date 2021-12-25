package com.shade.decima.rtti.registry;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.NotNull;

import java.util.*;

public final class RTTITypeRegistry {
    private static final RTTITypeRegistry INSTANCE = new RTTITypeRegistry();

    private final List<RTTITypeProvider> providers = new ArrayList<>();
    private final Map<String, RTTIType<?>> nameToTypeCache = new HashMap<>();
    private final Map<RTTIType<?>, String> typeToNameCache = new HashMap<>();

    private RTTITypeRegistry() {
        for (RTTITypeProvider provider : ServiceLoader.load(RTTITypeProvider.class)) {
            providers.add(provider);
        }

        for (RTTITypeProvider provider : providers) {
            provider.initialize(this);
        }
    }

    @NotNull
    public static RTTITypeRegistry getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T> RTTIType<T> get(@NotNull String name) {
        RTTIType<?> type = nameToTypeCache.get(name);


        if (type == null) {
            for (RTTITypeProvider provider : providers) {
                type = provider.lookup(this, name);

                if (type != null) {
                    register(type, name);
                    break;
                }
            }
        }

        if (type == null) {
            throw new IllegalArgumentException("Can't find type '" + name + "' in the registry");
        }

        return (RTTIType<T>) type;
    }

    @NotNull
    public String getName(@NotNull RTTIType<?> type) {
        final String name = typeToNameCache.get(type);
        if (name == null) {
            throw new IllegalArgumentException("Type " + type + " not present in the registry");
        }
        return name;
    }

    public void register(@NotNull RTTIType<?> type, @NotNull String name, @NotNull String... aliases) {
        register(type, name);

        for (String alias : aliases) {
            register(type, alias);
        }
    }

    public void register(@NotNull RTTIType<?> type, @NotNull String name) {
        if (nameToTypeCache.containsKey(name)) {
            throw new IllegalArgumentException("Type '" + name + "' already present in the registry");
        }
        nameToTypeCache.put(name, type);
        typeToNameCache.put(type, name);
    }
}
