package com.shade.decima.model.rtti.registry.providers;

import com.google.gson.Gson;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.messages.RTTIMessageHandler;
import com.shade.decima.model.rtti.messages.RTTIMessageHandlers;
import com.shade.decima.model.rtti.registry.RTTITypeProvider;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.RTTITypeEnumFlags;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ExternalTypeProvider implements RTTITypeProvider {
    private static final Logger log = LoggerFactory.getLogger(ExternalTypeProvider.class);

    private final Map<String, Map<String, Object>> declarations = new HashMap<>();
    private final Map<String, Map<String, Object>> messages = new HashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public void initialize(@NotNull RTTITypeRegistry registry, @NotNull Path externalTypeInfo, @NotNull GameType gameType) {
        try (BufferedReader reader = Files.newBufferedReader(externalTypeInfo)) {
            declarations.putAll(new Gson().fromJson(reader, Map.class));
        } catch (IOException e) {
            throw new RuntimeException("Error loading types definition from file " + externalTypeInfo, e);
        }
        for (String type : declarations.keySet()) {
            final RTTIType<?> lookup = lookup(registry, type);
            if (lookup != null) {
                registry.define(this, lookup);
            }
        }

        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        final Reflections reflections = new Reflections("com.shade.decima");

        final Set<Class<?>> types = new HashSet<>();
        types.addAll(reflections.getTypesAnnotatedWith(RTTIMessageHandlers.class));
        types.addAll(reflections.getTypesAnnotatedWith(RTTIMessageHandler.class));

        for (Class<?> type : types) {
            try {
                final RTTIMessageHandler[] annotations = type.getAnnotationsByType(RTTIMessageHandler.class);
                final Object instance = lookup.findConstructor(type, MethodType.methodType(void.class)).invoke();

                for (RTTIMessageHandler annotation : annotations) {
                    if (annotation.game() != gameType) {
                        continue;
                    }

                    messages
                        .computeIfAbsent(annotation.type(), key -> new HashMap<>())
                        .put(annotation.message(), instance);
                }
            } catch (Throwable e) {
                log.error("Error constructing message handler", e);
            }
        }
    }

    @Nullable
    @Override
    public RTTIType<?> lookup(@NotNull RTTITypeRegistry registry, @NotNull String name) {
        final Map<String, Object> definition = declarations.get(name);

        if (definition == null) {
            return null;
        }

        return switch ((String) definition.get("type")) {
            case "class" -> loadClassType(name, definition);
            case "enum" -> loadEnumType(name, definition);
            case "enum flags" -> loadEnumFlagsType(name, definition);
            case "primitive" -> loadPrimitiveType(registry, name, definition);
            case "container", "reference" -> null;
            default -> throw new IllegalStateException("Unsupported type '" + definition.get("type") + "'");
        };
    }

    @Override
    public void resolve(@NotNull RTTITypeRegistry registry, @NotNull RTTIType<?> type) {
        final Map<String, Object> definition = Objects.requireNonNull(declarations.get(RTTITypeRegistry.getFullTypeName(type)));

        switch ((String) definition.get("type")) {
            case "class" -> resolveClassType(registry, (RTTITypeClass) type, definition);
            case "enum" -> resolveEnumType((RTTITypeEnum) type, definition);
            case "enum flags" -> resolveEnumFlagsType((RTTITypeEnumFlags) type, definition);
            case "primitive" -> {
            }
            default -> throw new IllegalStateException("Unsupported type '" + definition.get("type") + "'");
        }
    }

    @NotNull
    private RTTITypeClass loadClassType(@NotNull String name, @NotNull Map<String, Object> definition) {
        final List<Map<String, Object>> basesInfo = getList(definition, "bases");
        final List<Map<String, Object>> membersInfo = getList(definition, "members");

        return new RTTITypeClass(
            name,
            new RTTITypeClass.Base[basesInfo.size()],
            new RTTITypeClass.Member[membersInfo.size()],
            new HashMap<>(),
            getInt(definition, "flags"),
            getInt(definition, "unknownC")
        );
    }

    @NotNull
    private RTTIType<?> loadEnumType(@NotNull String name, @NotNull Map<String, Object> definition) {
        final List<Object> valuesInfo = getList(definition, "members");
        final int size = getInt(definition, "size");
        return new RTTITypeEnum(name, new RTTITypeEnum.Constant[valuesInfo.size()], size);
    }

    @NotNull
    private RTTIType<?> loadEnumFlagsType(@NotNull String name, @NotNull Map<String, Object> definition) {
        final List<Object> valuesInfo = getList(definition, "members");
        final int size = getInt(definition, "size");
        return new RTTITypeEnumFlags(name, new RTTITypeEnumFlags.Constant[valuesInfo.size()], size);
    }

    @Nullable
    private RTTIType<?> loadPrimitiveType(@NotNull RTTITypeRegistry registry, @NotNull String name, @NotNull Map<String, Object> definition) {
        final String parent = getString(definition, "parent_type");

        if (name.equals(parent)) {
            // Found an internal type, we couldn't load it
            return null;
        }

        return new DelegatingPrimitiveType<>(name, registry.find(parent, false));
    }

    private void resolveClassType(@NotNull RTTITypeRegistry registry, @NotNull RTTITypeClass type, @NotNull Map<String, Object> definition) {
        final List<Map<String, Object>> basesInfo = getList(definition, "bases");
        final List<Map<String, Object>> membersInfo = getList(definition, "members");
        final List<String> messagesInfo = getList(definition, "messages");

        for (int i = 0; i < basesInfo.size(); i++) {
            final var baseInfo = basesInfo.get(i);
            final var baseType = (RTTITypeClass) registry.find(getString(baseInfo, "name"));
            final var baseOffset = getInt(baseInfo, "offset");

            type.getBases()[i] = new RTTITypeClass.Base(type,
                baseType,
                baseOffset
            );
        }

        for (int i = 0; i < membersInfo.size(); i++) {
            final var memberInfo = membersInfo.get(i);
            final var memberType = registry.find(getString(memberInfo, "type"));
            final var memberName = getString(memberInfo, "name");
            final var memberCategory = getString(memberInfo, "category");
            final var memberOffset = getInt(memberInfo, "offset");
            final var memberFlags = getInt(memberInfo, "flags");

            type.getMembers()[i] = new RTTITypeClass.Member(type,
                memberType,
                memberName,
                memberCategory.isEmpty() ? null : memberCategory,
                memberOffset,
                memberFlags
            );
        }

        for (String message : messagesInfo) {
            final Map<String, Object> handlers = messages.get(type.getName());

            if (handlers != null) {
                final Object handler = handlers.get(message);

                if (handler != null) {
                    type.getMessages().put(message, handler);
                    log.debug("Found message handler for type '{}' that handles message '{}'", type, message);
                    continue;
                }
            }

            log.debug("Can't find message handler for type '{}' that handles message '{}'", type.getName(), message);
        }
    }

    private void resolveEnumType(@NotNull RTTITypeEnum type, @NotNull Map<String, Object> definition) {
        final List<Map<String, Object>> valuesInfo = getList(definition, "members");

        for (int i = 0; i < valuesInfo.size(); i++) {
            final var valueInfo = valuesInfo.get(i);
            final var valueName = getString(valueInfo, "name");
            final var valueData = getInt(valueInfo, "value");
            type.getConstants()[i] = new RTTITypeEnum.Constant(type, valueName, valueData);
        }
    }

    private void resolveEnumFlagsType(@NotNull RTTITypeEnumFlags type, @NotNull Map<String, Object> definition) {
        final List<Map<String, Object>> valuesInfo = getList(definition, "members");

        for (int i = 0; i < valuesInfo.size(); i++) {
            final var valueInfo = valuesInfo.get(i);
            final var valueName = getString(valueInfo, "name");
            final var valueData = getInt(valueInfo, "value");
            type.getConstants()[i] = new RTTITypeEnumFlags.Constant(type, valueName, valueData);
        }
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static <T> List<T> getList(@NotNull Map<String, Object> map, @NotNull String key) {
        final List<T> list = (List<T>) map.get(key);
        return list == null ? Collections.emptyList() : list;
    }

    @NotNull
    private static String getString(@NotNull Map<String, Object> map, @NotNull String key) {
        return (String) map.get(key);
    }

    private static int getInt(@NotNull Map<String, Object> map, @NotNull String key) {
        return ((Number) map.get(key)).intValue();
    }

    @NotNull
    private static Number getNumber(@NotNull Map<String, Object> map, @NotNull String key) {
        return (Number) map.get(key);
    }

    private static class DelegatingPrimitiveType<T> implements RTTIType<T> {
        private final String name;
        private final RTTIType<T> delegate;

        public DelegatingPrimitiveType(@NotNull String name, @NotNull RTTIType<T> delegate) {
            this.name = name;
            this.delegate = delegate;
        }

        @NotNull
        @Override
        public T read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            return delegate.read(registry, buffer);
        }

        @Override
        public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull T value) {
            delegate.write(registry, buffer, value);
        }

        @NotNull
        @Override
        public String getName() {
            return name;
        }

        @NotNull
        @Override
        public Kind getKind() {
            return delegate.getKind();
        }

        @NotNull
        @Override
        public Class<T> getComponentType() {
            return delegate.getComponentType();
        }
    }
}
