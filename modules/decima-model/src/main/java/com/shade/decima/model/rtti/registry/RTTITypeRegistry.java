package com.shade.decima.model.rtti.registry;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeSerialized;
import com.shade.util.NotNull;

import java.io.IOException;
import java.util.*;

public class RTTITypeRegistry implements Iterable<RTTIType<?>> {
    private final List<RTTITypeProvider> providers = new ArrayList<>();

    private final Map<String, RTTIType<?>> cacheByName = new HashMap<>();
    private final Map<Long, RTTIType<?>> cacheByHash = new HashMap<>();
    private final Map<Class<?>, RTTIType<?>> cacheByClass = new HashMap<>();
    private final Map<RTTIType<?>, Long> hashByType = new HashMap<>();

    private final Deque<PendingType> pendingTypes = new ArrayDeque<>();

    public RTTITypeRegistry(@NotNull ProjectContainer container) throws IOException {
        for (RTTITypeProvider provider : ServiceLoader.load(RTTITypeProvider.class)) {
            provider.initialize(this, container);
            providers.add(provider);
        }

        resolvePending();
        computeHashes();
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T extends RTTIType<?>> T find(@NotNull Class<?> clazz) {
        RTTIType<?> type = cacheByClass.get(clazz);

        if (type == null) {
            for (RTTITypeProvider provider : providers) {
                type = provider.lookup(this, clazz);

                if (type != null) {
                    cacheByClass.put(clazz, type);
                    break;
                }
            }
        }

        if (type == null) {
            throw new IllegalArgumentException("Can't find type that represents class '" + clazz + "' in the registry");
        }

        return (T) type;
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
    public <T extends RTTIType<?>> T find(@NotNull String name) {
        return find(name, true);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T extends RTTIType<?>> T find(@NotNull String name, boolean resolve) {
        RTTIType<?> type = cacheByName.get(name);

        if (type != null) {
            return (T) type;
        }

        final boolean isRoot = pendingTypes.isEmpty();

        for (RTTITypeProvider provider : providers) {
            type = provider.lookup(this, name);

            if (type != null) {
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

        return (T) type;
    }

    public long getHash(@NotNull RTTIType<?> type) {
        final Long hash = hashByType.get(type);

        if (hash == null) {
            throw new IllegalArgumentException("Can't find type '" + type.getFullTypeName() + "' in the registry");
        }

        return hash;
    }

    @Override
    public Iterator<RTTIType<?>> iterator() {
        return cacheByName.values().iterator();
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
        final String name = type.getFullTypeName();
        if (cacheByName.putIfAbsent(name, type) != null) {
            throw new IllegalArgumentException("Type '" + name + "' is already present in the registry");
        }
        cacheByName.put(name, type);
        pendingTypes.offer(new PendingType(provider, type));
    }

    private record PendingType(@NotNull RTTITypeProvider provider, @NotNull RTTIType<?> type) {}
}
