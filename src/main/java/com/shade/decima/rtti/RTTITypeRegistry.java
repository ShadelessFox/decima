package com.shade.decima.rtti;

import com.shade.decima.rtti.types.*;
import com.shade.decima.util.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class RTTITypeRegistry {
    private static final Map<String, RTTIType<?>> types = new HashMap<>();
    private static final Map<String, GenericTypeConstructor<?, ?>> genericTypeConstructors = new HashMap<>();

    static {
        register(new RTTITypeUInt8());
        register(new RTTITypeUInt16());
        register(new RTTITypeUInt32());
        register(new RTTITypeUInt64());
        register(new RTTITypeFloat32());
        register(new RTTITypeBoolean());
        register(new RTTITypeString());
        register(new RTTITypeUUID());

        registerGeneric("Array", RTTITypeArray::new);
        registerGeneric("Ref", RTTITypeRef::new);
        registerGeneric("UUIDRef", RTTITypeUUIDRef::new);
        registerGeneric("HashMap", RTTITypeHashMap::new);
    }

    private RTTITypeRegistry() {
    }

    public static synchronized void register(@NotNull RTTIType<?> type) {
        register(getName(type), type);

        if (type instanceof RTTITypeWithAlias) {
            for (String alias : ((RTTITypeWithAlias<?>) type).getAliases()) {
                register(getName(alias, type), type);
            }
        }
    }

    private static void register(@NotNull String name, @NotNull RTTIType<?> type) {
        if (types.containsKey(name)) {
            throw new IllegalArgumentException("Type with name '" + name + "' already present in the registry");
        }
        types.put(name, type);
    }

    public static synchronized <T, U> void registerGeneric(@NotNull String name, @NotNull GenericTypeConstructor<T, U> constructor) {
        genericTypeConstructors.put(name, constructor);
    }

    public static boolean contains(@NotNull String name) {
        return types.containsKey(name);
    }

    @SuppressWarnings("unchecked")
    public static synchronized <U> RTTIType<?> findTemplate(@NotNull String name, @NotNull RTTIType<U> underlyingType) {
        final String fullName = name + "<" + getName(underlyingType) + ">";
        if (types.containsKey(fullName)) {
            return types.get(fullName);
        }
        final GenericTypeConstructor<?, U> constructor = (GenericTypeConstructor<?, U>) genericTypeConstructors.get(name);
        if (constructor == null) {
            throw new IllegalArgumentException("Templated type with name '" + name + "' is missing in the registry");
        }
        final RTTIGenericType<?, ?> type = constructor.apply(underlyingType);
        register(type);
        return type;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> RTTIType<T> find(@NotNull String name) {
        final RTTIType<?> type = types.get(name);
        if (type == null) {
            throw new IllegalArgumentException("Type with name '" + name + "' is missing in the registry");
        }
        return (RTTIType<T>) type;
    }

    @NotNull
    public static String getName(@NotNull RTTIType<?> type) {
        if (type instanceof RTTIGenericType) {
            return type.getName() + "<" + getName(((RTTIGenericType<?, ?>) type).getUnderlyingType()) + ">";
        }
        return type.getName();
    }

    @NotNull
    public static String getName(@NotNull String alias, @NotNull RTTIType<?> type) {
        if (type instanceof RTTIGenericType) {
            return alias + "<" + getName(((RTTIGenericType<?, ?>) type).getUnderlyingType()) + ">";
        }
        return alias;
    }

    private interface GenericTypeConstructor<T, U> extends Function<RTTIType<U>, RTTIGenericType<T, U>> {
    }
}
