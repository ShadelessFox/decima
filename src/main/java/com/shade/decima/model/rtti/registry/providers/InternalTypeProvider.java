package com.shade.decima.model.rtti.registry.providers;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.registry.RTTITypeProvider;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.IOUtils;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class InternalTypeProvider implements RTTITypeProvider {
    private static final Logger log = LoggerFactory.getLogger(InternalTypeProvider.class);

    private final Map<String, MethodHandle> templates = new HashMap<>();

    @Override
    public void initialize(@NotNull RTTITypeRegistry registry, @NotNull Path externalTypeInfo, @NotNull GameType gameType) {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        final Set<Class<?>> types = new Reflections("com.shade.decima").getTypesAnnotatedWith(RTTIDefinition.class);

        for (Class<?> type : types) {
            final RTTIDefinition definition = type.getAnnotation(RTTIDefinition.class);

            try {
                // TODO: Perhaps it shouldn't be called here?
                registry.define(this, (RTTIType<?>) lookup.findConstructor(type, MethodType.methodType(void.class, String.class)).invoke(definition.name()));
                for (String alias : definition.aliases()) {
                    registry.define(this, (RTTIType<?>) lookup.findConstructor(type, MethodType.methodType(void.class, String.class)).invoke(alias));
                }
                log.debug("Registered type '{}'", definition.name());
                continue;
            } catch (Throwable ignored) {
            }

            try {
                registerTemplate(lookup.findConstructor(type, MethodType.methodType(void.class, String.class, RTTIType.class)), definition.name());
                for (String alias : definition.aliases()) {
                    registerTemplate(lookup.findConstructor(type, MethodType.methodType(void.class, String.class, RTTIType.class)), alias);
                }
                log.debug("Registered template type '{}'", definition.name());
                continue;
            } catch (Throwable ignored) {
            }

            throw new IllegalArgumentException("Can't find suitable public constructor to construct type " + type.getName());
        }
    }

    @Nullable
    @Override
    public RTTIType<?> lookup(@NotNull RTTITypeRegistry registry, @NotNull String name) {
        if (isTemplateTypeName(name)) {
            final String templateTypeName = getTemplateTypeName(name);
            final String templateParameterTypeName = getTemplateParameterTypeName(name);

            final MethodHandle templateTypeConstructor = templates.get(templateTypeName);
            final RTTIType<?> templateParameterType = registry.find(templateParameterTypeName, false);

            if (templateTypeConstructor == null) {
                throw new IllegalArgumentException("Type with name '" + templateTypeName + "' is either not templated or missing in the registry");
            }

            try {
                return (RTTIType<?>) templateTypeConstructor.invoke(templateTypeName, templateParameterType);
            } catch (Throwable e) {
                throw new RuntimeException("Error constructing templated RTTI type " + templateTypeName + "<" + templateParameterTypeName + ">", e);
            }
        }

        return switch (name) {
            case "uint8", "int8", "byte" -> new PrimitiveType<>(name, Byte.class, ByteBuffer::get, ByteBuffer::put);
            case "uint16", "int16", "short", "HalfFloat", "wchar" -> new PrimitiveType<>(name, Short.class, ByteBuffer::getShort, ByteBuffer::putShort);
            case "uint", "uint32", "int32", "int", "ucs4" -> new PrimitiveType<>(name, Integer.class, ByteBuffer::getInt, ByteBuffer::putInt);
            case "uint64", "int64", "long" -> new PrimitiveType<>(name, Long.class, ByteBuffer::getLong, ByteBuffer::putLong);
            case "uint128" -> new PrimitiveType<>(name, BigInteger.class, buf -> new BigInteger(IOUtils.getBytesExact(buf, 16)), (buf, val) -> buf.put(val.toByteArray()));
            case "float" -> new PrimitiveType<>(name, Float.class, ByteBuffer::getFloat, ByteBuffer::putFloat);
            case "double" -> new PrimitiveType<>(name, Double.class, ByteBuffer::getDouble, ByteBuffer::putDouble);
            case "bool" -> new PrimitiveType<>(name, Boolean.class, buf -> buf.get() > 0, (buf, val) -> buf.put(val ? (byte) 1 : 0));
            default -> null;
        };
    }

    @Override
    public void resolve(@NotNull RTTITypeRegistry registry, @NotNull RTTIType<?> type) {
        // Nothing to resolve
    }

    private void registerTemplate(@NotNull MethodHandle constructor, @NotNull String name) {
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
        private final String name;
        private final Class<T> type;
        private final Function<ByteBuffer, T> reader;
        private final BiConsumer<ByteBuffer, T> writer;

        public PrimitiveType(@NotNull String name, @NotNull Class<T> type, @NotNull Function<ByteBuffer, T> reader, @NotNull BiConsumer<ByteBuffer, T> writer) {
            this.name = name;
            this.type = type;
            this.reader = reader;
            this.writer = writer;
        }

        @NotNull
        @Override
        public T read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            return reader.apply(buffer);
        }

        @Override
        public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull T value) {
            writer.accept(buffer, value);
        }

        @NotNull
        @Override
        public String getName() {
            return name;
        }

        @NotNull
        @Override
        public Kind getKind() {
            return Kind.PRIMITIVE;
        }

        @NotNull
        @Override
        public Class<T> getComponentType() {
            return type;
        }
    }
}
