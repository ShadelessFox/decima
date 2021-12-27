package com.shade.decima.rtti.registry;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class RTTITypeRegistry {
    private static final Logger log = LoggerFactory.getLogger(RTTITypeRegistry.class);
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
            log.debug("Type '{}' was not found in the registry", name);

            for (RTTITypeProvider provider : providers) {
                log.debug("Attempting to lookup type '{}' using provider {}", name, provider);
                type = provider.lookup(this, name);

                if (type != null) {
                    log.debug("Type '{}' was found using provider {}", name, provider);
                    register(type, name);
                    break;
                }

                log.debug("Type '{}' was not found using provider {}", name, provider);
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
            throw new IllegalArgumentException("Type " + type.getClass() + " not present in the registry");
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
