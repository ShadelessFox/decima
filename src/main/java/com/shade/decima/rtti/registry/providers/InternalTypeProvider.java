package com.shade.decima.rtti.registry.providers;

import com.shade.decima.rtti.RTTIDefinition;
import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.registry.RTTITypeProvider;
import com.shade.decima.rtti.registry.RTTITypeRegistry;
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

public class InternalTypeProvider implements RTTITypeProvider {
    private static final Map<String, MethodHandle> templates = new HashMap<>();

    @Override
    public void initialize(@NotNull RTTITypeRegistry registry) {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        final Set<Class<?>> types = new Reflections("com.shade.decima").getTypesAnnotatedWith(RTTIDefinition.class);

        for (Class<?> type : types) {
            final RTTIDefinition definition = type.getAnnotation(RTTIDefinition.class);
            final TypeVariable<? extends Class<?>>[] parameters = type.getTypeParameters();

            if (parameters.length == 0) {
                try {
                    registry.register(
                        (RTTIType<?>) lookup.findConstructor(type, MethodType.methodType(void.class)).invoke(),
                        definition.name(),
                        definition.aliases()
                    );
                } catch (Throwable e) {
                    throw new RuntimeException("Can't construct type " + type.getName(), e);
                }
            }

            if (parameters.length == 1) {
                try {
                    registerTemplate(
                        lookup.findConstructor(type, MethodType.methodType(void.class, RTTIType.class)),
                        definition.name(),
                        definition.aliases()
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Can't construct template type " + type.getName(), e);
                }
            }
        }
    }

    @Nullable
    @Override
    public RTTIType<?> lookup(@NotNull RTTITypeRegistry registry, @NotNull String name) {
        if (isTemplateTypeName(name)) {
            final String templateTypeName = getTemplateTypeName(name);
            final String templateParameterTypeName = getTemplateParameterTypeName(name);

            final MethodHandle templateTypeConstructor = templates.get(templateTypeName);
            final RTTIType<Object> templateParameterType = registry.get(templateParameterTypeName);

            if (templateTypeConstructor == null) {
                throw new IllegalArgumentException("Type with name '" + templateTypeName + "' is either not templated or missing in the registry");
            }

            try {
                return (RTTIType<?>) templateTypeConstructor.invoke(templateParameterType);
            } catch (Throwable e) {
                throw new RuntimeException("Error constructing templated RTTI type " + templateTypeName + "<" + templateParameterTypeName + ">", e);
            }
        }

        return switch (name) {
            case "uint8", "int8", "byte" -> new PrimitiveType<>(Byte.class, ByteBuffer::get, ByteBuffer::put);
            case "uint16", "int16", "short" -> new PrimitiveType<>(Short.class, ByteBuffer::getShort, ByteBuffer::putShort);
            case "uint32", "int32", "int" -> new PrimitiveType<>(Integer.class, ByteBuffer::getInt, ByteBuffer::putInt);
            case "uint64", "int64", "long" -> new PrimitiveType<>(Long.class, ByteBuffer::getLong, ByteBuffer::putLong);
            case "float" -> new PrimitiveType<>(Float.class, ByteBuffer::getFloat, ByteBuffer::putFloat);
            case "double" -> new PrimitiveType<>(Double.class, ByteBuffer::getDouble, ByteBuffer::putDouble);
            case "bool" -> new PrimitiveType<>(Boolean.class, buf -> buf.get() > 0, (buf, val) -> buf.put(val ? (byte) 1 : 0));
            default -> null;
        };
    }

    private static void registerTemplate(@NotNull MethodHandle constructor, @NotNull String name, @NotNull String... aliases) {
        registerTemplate(constructor, name);

        for (String alias : aliases) {
            registerTemplate(constructor, alias);
        }
    }

    private static void registerTemplate(@NotNull MethodHandle constructor, @NotNull String name) {
        if (templates.containsKey(name)) {
            throw new IllegalArgumentException("Template type '" + name + "' already present in the registry");
        }
        templates.put(name, constructor);
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
