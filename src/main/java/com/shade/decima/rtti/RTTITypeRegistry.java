package com.shade.decima.rtti;

import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;
import org.reflections.Reflections;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.TypeVariable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class RTTITypeRegistry {
    private static final Map<String, RTTIType<?>> types = new HashMap<>();
    private static final Map<String, MethodHandle> generics = new HashMap<>();
    private static final Map<RTTIType<?>, String> names = new HashMap<>();

    static {
        register(new PrimitiveType<>(Boolean.class, buf -> buf.get() > 0, (buf, val) -> buf.put(val ? (byte) 1 : 0)), "bool");
        register(new PrimitiveType<>(Byte.class, ByteBuffer::get, ByteBuffer::put), "uint8");
        register(new PrimitiveType<>(Short.class, ByteBuffer::getShort, ByteBuffer::putShort), "uint16");
        register(new PrimitiveType<>(Integer.class, ByteBuffer::getInt, ByteBuffer::putInt), "uint32", "int");
        register(new PrimitiveType<>(Long.class, ByteBuffer::getLong, ByteBuffer::putLong), "uint64");
        register(new PrimitiveType<>(Float.class, ByteBuffer::getFloat, ByteBuffer::putFloat), "float");
        register(new PrimitiveType<>(Double.class, ByteBuffer::getDouble, ByteBuffer::putDouble), "double");
        registerDefinitions();
    }

    private RTTITypeRegistry() {
    }

    @NotNull
    public static <T> RTTIType<T> get(@NotNull String name) {
        return get(name, null);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> RTTIType<T> get(@NotNull String name, @Nullable Function<String, RTTIType<?>> fallback) {
        RTTIType<?> type = types.get(name);

        if (type == null && isTemplateTypeName(name)) {
            final String templateTypeName = getTemplateTypeName(name);
            final String templateParameterTypeName = getTemplateParameterTypeName(name);
            final MethodHandle constructor = generics.get(templateTypeName);

            if (constructor == null) {
                throw new IllegalArgumentException("Type with name '" + templateTypeName + "' is either not templated or missing in the registry");
            }

            try {
                type = (RTTIType<?>) constructor.invoke(get(templateParameterTypeName, fallback));
            } catch (Throwable e) {
                throw new RuntimeException("Error constructing templated RTTI type " + templateTypeName, e);
            }

            register(type, name);
        }

        if (type == null && fallback != null) {
            type = fallback.apply(name);
        }

        if (type == null) {
            throw new IllegalArgumentException("Type with name '" + name + "' is missing in the registry");
        }

        return (RTTIType<T>) type;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> RTTIType<T> get(@NotNull Class<T> cls) {
        for (RTTIType<?> type : types.values()) {
            if (type.getType().equals(cls)) {
                return (RTTIType<T>) type;
            }
        }
        throw new IllegalArgumentException("Type that represents '" + cls.getName() + "' is missing in the registry");
    }

    @NotNull
    public static String getName(@NotNull RTTIType<?> type) {
        final String name = names.get(type);
        if (name == null) {
            throw new IllegalArgumentException("Type " + type + " not present in the registry");
        }
        return name;
    }

    public static void register(@NotNull RTTIType<?> type, @NotNull String name, @NotNull String... aliases) {
        register(type, name);

        for (String alias : aliases) {
            register(type, alias);
        }
    }

    private static void register(@NotNull RTTIType<?> type, @NotNull String name) {
        if (types.containsKey(name)) {
            throw new IllegalArgumentException("Type with name '" + name + "' already present in the registry");
        }
        types.put(name, type);
        names.put(type, name);
    }

    private static void register(@NotNull MethodHandle constructor, @NotNull String name, @NotNull String... aliases) {
        register(constructor, name);

        for (String alias : aliases) {
            register(constructor, alias);
        }
    }

    private static void register(@NotNull MethodHandle constructor, @NotNull String name) {
        if (generics.containsKey(name)) {
            throw new IllegalArgumentException("Templated type with name '" + name + "' already present in the registry");
        }
        generics.put(name, constructor);
    }

    private static void registerDefinitions() {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        final Set<Class<?>> types = new Reflections(RTTITypeRegistry.class.getPackageName()).getTypesAnnotatedWith(RTTIDefinition.class);

        for (Class<?> type : types) {
            final RTTIDefinition definition = type.getAnnotation(RTTIDefinition.class);
            final TypeVariable<? extends Class<?>>[] parameters = type.getTypeParameters();

            if (parameters.length == 0) {
                try {
                    final MethodHandle constructor = lookup.findConstructor(type, MethodType.methodType(void.class));
                    register((RTTIType<?>) constructor.invoke(), definition.name(), definition.aliases());
                } catch (Throwable e) {
                    throw new RuntimeException("Can't construct RTTI type " + type.getName(), e);
                }
            }

            if (parameters.length == 1) {
                try {
                    final MethodHandle constructor = lookup.findConstructor(type, MethodType.methodType(void.class, RTTIType.class));
                    register(constructor, definition.name(), definition.aliases());
                } catch (Exception e) {
                    throw new RuntimeException("Can't construct templated RTTI type " + type.getName(), e);
                }
            }
        }
    }

    private static boolean isTemplateTypeName(@NotNull String name) {
        return name.indexOf('<') > 0;
    }

    @NotNull
    private static String getTemplateTypeName(@NotNull String name) {
        final int start = name.indexOf('<');
        if (start <= 0) {
            throw new IllegalArgumentException("Invalid template name: '" + name + "'");
        }
        return name.substring(0, start);
    }

    @NotNull
    private static String getTemplateParameterTypeName(@NotNull String name) {
        final int start = name.indexOf('<');
        final int end = name.lastIndexOf('>');
        if (start <= 0 || end < start + 1) {
            throw new IllegalArgumentException("Invalid template name: '" + name + "'");
        }
        return name.substring(start + 1, end);
    }

    private static class PrimitiveType<T> implements RTTIType<T> {
        private final Class<T> type;
        private final Function<ByteBuffer, T> reader;
        private final BiConsumer<ByteBuffer, T> writer;

        public PrimitiveType(@NotNull Class<T> type, @NotNull Function<ByteBuffer, T> reader, @NotNull BiConsumer<ByteBuffer, T> writer) {
            this.type = type;
            this.reader = reader;
            this.writer = writer;
        }

        @NotNull
        @Override
        public T read(@NotNull ByteBuffer buffer) {
            return reader.apply(buffer);
        }

        @Override
        public void write(@NotNull ByteBuffer buffer, @NotNull T value) {
            writer.accept(buffer, value);
        }

        @NotNull
        @Override
        public Class<T> getType() {
            return type;
        }
    }
}
