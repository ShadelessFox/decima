package com.shade.decima.model.rtti.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeContainer;
import com.shade.decima.model.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

public class RTTITypeRegistry {
    private static final Logger log = LoggerFactory.getLogger(RTTITypeRegistry.class);

    private final List<RTTITypeProvider> providers;
    private final BiMap<String, RTTIType<?>> cacheByName;
    private final BiMap<Long, RTTIType<?>> cacheByHash;

    private final Deque<PendingType> pendingTypes = new ArrayDeque<>();

    public RTTITypeRegistry(@NotNull Path externalTypeInfo, @NotNull GameType gameType) {
        this.providers = new ArrayList<>();
        this.cacheByName = HashBiMap.create();
        this.cacheByHash = HashBiMap.create();

        for (RTTITypeProvider provider : ServiceLoader.load(RTTITypeProvider.class)) {
            provider.initialize(this, externalTypeInfo, gameType);
            providers.add(provider);
        }

        resolvePending();
    }

    @NotNull
    public static String getFullTypeName(@NotNull RTTIType<?> type) {
        if (type instanceof RTTITypeContainer<?> specialized) {
            return "%s<%s>".formatted(type.getName(), getFullTypeName(specialized.getContainedType()));
        }
        return type.getName();
    }

    @NotNull
    public RTTIType<?> find(long hash) {
        return find(hash, cacheByHash.isEmpty());
    }

    @NotNull
    public RTTIType<?> find(long hash, boolean recompute) {
        if (recompute) {
            cacheByHash.clear();
            cacheByName.values().forEach(this::computeHash);
        }

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

    private void resolvePending() {
        while (!pendingTypes.isEmpty()) {
            final PendingType type = pendingTypes.element();
            type.provider().resolve(this, type.type());
            pendingTypes.remove();
        }
    }

    private void computeHash(@NotNull RTTIType<?> type) {
        final RTTITypeDumper dumper = new RTTITypeDumper();
        final long[] id = dumper.getTypeId(type);
        cacheByHash.put(id[0], type);
    }

    public void define(@NotNull RTTITypeProvider provider, @NotNull RTTIType<?> type) {
        final String name = getFullTypeName(type);
        if (cacheByName.putIfAbsent(name, type) != null) {
            throw new IllegalArgumentException("Type '" + name + "' is already present in the registry");
        }
        cacheByName.put(name, type);
        pendingTypes.offer(new PendingType(provider, type));
    }

    private static record PendingType(@NotNull RTTITypeProvider provider, @NotNull RTTIType<?> type) {
    }
}
