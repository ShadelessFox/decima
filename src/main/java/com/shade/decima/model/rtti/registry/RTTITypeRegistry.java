package com.shade.decima.model.rtti.registry;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeParameterized;
import com.shade.decima.model.rtti.RTTITypeSerialized;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RTTITypeRegistry {
    private static final Logger log = LoggerFactory.getLogger(RTTITypeRegistry.class);

    private final List<RTTITypeProvider> providers;
    private final Map<String, RTTIType<?>> cacheByName;
    private final Map<Long, RTTIType<?>> cacheByHash;
    private final Map<RTTIType<?>, Long> hashByType;

    private final Deque<PendingType> pendingTypes = new ArrayDeque<>();

    public RTTITypeRegistry(@NotNull ProjectContainer container) {
        this.providers = new ArrayList<>();
        this.cacheByName = new HashMap<>();
        this.cacheByHash = new HashMap<>();
        this.hashByType = new HashMap<>();

        for (RTTITypeProvider provider : ServiceLoader.load(RTTITypeProvider.class)) {
            provider.initialize(this, container);
            providers.add(provider);
        }

        resolvePending();
        computeHashes();
    }

    @NotNull
    public static String getFullTypeName(@NotNull RTTIType<?> type) {
        if (type instanceof RTTITypeParameterized<?, ?> parameterized) {
            return type.getTypeName() + '<' + getFullTypeName(parameterized.getComponentType()) + '>';
        } else {
            return type.getTypeName();
        }
    }

    @NotNull
    public RTTIType<?> find(long hash) {
        final RTTIType<?> type = cacheByHash.get(hash);

        if (type == null) {
            throw new IllegalArgumentException("Can't find type with hash 0x" + Long.toHexString(hash) + " in the registry");
        }

        return type;
    }

    @NotNull
    public RTTIType<?> find(@NotNull String name) {
        return find(name, true);
    }

    @NotNull
    public RTTIType<?> find(@NotNull String name, boolean resolve) {
        RTTIType<?> type = cacheByName.get(name);

        if (type != null) {
            return type;
        }

        final boolean isRoot = pendingTypes.isEmpty();

        for (RTTITypeProvider provider : providers) {
            type = provider.lookup(this, name);

            if (type != null) {
                log.debug("Type '{}' was found using provider {}", name, provider);

                define(provider, type);

                break;
            }
        }

        if (type == null) {
            throw new IllegalArgumentException("Can't find type '" + name + "' in the registry");
        }

        if (isRoot && resolve) {
            resolvePending();
        }

        return type;
    }

    public long getHash(@NotNull RTTIType<?> type) {
        final Long hash = hashByType.get(type);

        if (hash == null) {
            throw new IllegalArgumentException("Can't find type '" + getFullTypeName(type) + "' in the registry");
        }

        return hash;
    }

    private void resolvePending() {
        while (!pendingTypes.isEmpty()) {
            final PendingType type = pendingTypes.element();
            type.provider().resolve(this, type.type());
            pendingTypes.remove();
        }
    }

    private void computeHashes() {
        for (RTTIType<?> type : cacheByName.values()) {
            if (type instanceof RTTITypeSerialized serialized) {
                final RTTITypeSerialized.TypeId id = serialized.getTypeId();

                if (id != null) {
                    cacheByHash.put(id.low(), type);
                    hashByType.put(type, id.low());
                }
            }
        }
    }

    public void define(@NotNull RTTITypeProvider provider, @NotNull RTTIType<?> type) {
        final String name = getFullTypeName(type);
        if (cacheByName.putIfAbsent(name, type) != null) {
            throw new IllegalArgumentException("Type '" + name + "' is already present in the registry");
        }
        cacheByName.put(name, type);
        pendingTypes.offer(new PendingType(provider, type));
    }

    private record PendingType(@NotNull RTTITypeProvider provider, @NotNull RTTIType<?> type) {}
}
