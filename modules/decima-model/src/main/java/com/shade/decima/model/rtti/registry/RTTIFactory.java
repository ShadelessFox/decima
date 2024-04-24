package com.shade.decima.model.rtti.registry;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.IOException;
import java.util.*;

public class RTTIFactory {
    private final List<RTTITypeProvider> providers = new ArrayList<>();

    private final Map<String, RTTIType<?>> typeByName = new HashMap<>();
    private final Map<Long, RTTIType<?>> typeByHash = new HashMap<>();
    private final Map<Class<?>, RTTIType<?>> typeByClass = new HashMap<>();
    private final Map<RTTIType<?>, Long> hashByType = new HashMap<>();

    private final Deque<PendingType> pendingTypes = new ArrayDeque<>();

    public RTTIFactory(@NotNull ProjectContainer container) throws IOException {
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
        RTTIType<?> type = typeByClass.get(clazz);

        if (type == null) {
            for (RTTITypeProvider provider : providers) {
                type = provider.lookup(this, clazz);

                if (type != null) {
                    typeByClass.put(clazz, type);
                    break;
                }
            }
        }

        if (type == null) {
            throw new IllegalArgumentException("Can't find type that represents class '" + clazz + "' in the registry");
        }

        return (T) type;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends RTTIType<?>> T find(long hash) {
        return (T) typeByHash.get(hash);
    }

    @NotNull
    public <T extends RTTIType<?>> T find(@NotNull String name) {
        return find(name, true);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <T extends RTTIType<?>> T find(@NotNull String name, boolean resolve) {
        RTTIType<?> type = typeByName.get(name);

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

    private void resolvePending() {
        while (!pendingTypes.isEmpty()) {
            final PendingType type = pendingTypes.element();
            type.provider().resolve(this, type.type());
            pendingTypes.remove();
        }
    }

    private void computeHashes() {
        for (RTTIType<?> type : typeByName.values()) {
            if (type instanceof RTTIClass cls && cls.isInstanceOf("RTTIRefObject")) {
                final long hash = computeHash(cls);
                typeByHash.put(hash, cls);
                hashByType.put(cls, hash);
            }
        }
    }

    protected long computeHash(@NotNull RTTIType<?> type) {
        return new RTTITypeDumper().getTypeId(type).low();
    }

    public void define(@NotNull RTTITypeProvider provider, @NotNull RTTIType<?> type) {
        final String name = type.getFullTypeName();
        if (typeByName.putIfAbsent(name, type) != null) {
            throw new IllegalArgumentException("Type '" + name + "' is already present in the registry");
        }
        typeByName.put(name, type);
        pendingTypes.offer(new PendingType(provider, type));
    }

    private record PendingType(@NotNull RTTITypeProvider provider, @NotNull RTTIType<?> type) {}
}
