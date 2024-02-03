package com.shade.decima.model.rtti.registry.providers;

import com.google.gson.Gson;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.registry.RTTITypeProvider;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.decima.model.rtti.types.RTTITypeClass.MyField;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.RTTITypePrimitive;
import com.shade.platform.model.ExtensionRegistry;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class ExternalTypeProvider implements RTTITypeProvider {
    private static final Logger log = LoggerFactory.getLogger(ExternalTypeProvider.class);

    private final Map<String, Map<String, Object>> declarations = new HashMap<>();
    private final Map<String, Map<String, MessageHandler>> messages = new HashMap<>();
    private int version = 1;

    @SuppressWarnings("unchecked")
    @Override
    public void initialize(@NotNull RTTITypeRegistry registry, @NotNull ProjectContainer container) {
        try (Reader reader = IOUtils.newCompressedReader(container.getTypeMetadataPath())) {
            declarations.putAll(new Gson().fromJson(reader, Map.class));
        } catch (IOException e) {
            throw new RuntimeException("Error loading types definition from " + container.getTypeMetadataPath(), e);
        }

        if (declarations.containsKey("$spec")) {
            version = getInt(declarations.get("$spec"), "version");
            declarations.remove("$spec");
        }

        for (String type : declarations.keySet()) {
            if (lookup(registry, type) instanceof RTTIClass cls && isInstanceOf(type, "RTTIRefObject")) {
                registry.define(this, cls);
            }
        }

        for (var registration : ExtensionRegistry.getExtensions(MessageHandler.class, MessageHandlerRegistration.class)) {
            try {
                for (Type type : registration.metadata().types()) {
                    if (!IOUtils.contains(type.game(), container.getType())) {
                        continue;
                    }

                    messages
                        .computeIfAbsent(type.name(), key -> new HashMap<>())
                        .put(registration.metadata().message(), registration.get());
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
            case "enum" -> loadEnumType(name, definition, false);
            case "enum flags" -> loadEnumType(name, definition, true);
            case "primitive" -> loadPrimitiveType(registry, name, definition);
            case "container", "reference" -> null;
            default -> throw new IllegalStateException("Unsupported type '" + definition.get("type") + "'");
        };
    }

    @Override
    public void resolve(@NotNull RTTITypeRegistry registry, @NotNull RTTIType<?> type) {
        final Map<String, Object> definition = Objects.requireNonNull(declarations.get(type.getFullTypeName()));

        switch ((String) definition.get("type")) {
            case "class" -> resolveClassType(registry, (RTTITypeClass) type, definition);
            case "enum", "enum flags" -> resolveEnumType((RTTITypeEnum) type, definition);
            case "primitive" -> {
            }
            default -> throw new IllegalStateException("Unsupported type '" + definition.get("type") + "'");
        }
    }

    @NotNull
    private RTTITypeClass loadClassType(@NotNull String name, @NotNull Map<String, Object> definition) {
        return new RTTITypeClass(
            name,
            getInt(definition, version > 2 ? "version" : version > 1 ? "flags1" : "unknownC"),
            getInt(definition, version > 2 ? "flags" : version > 1 ? "flags2" : "flags")
        );
    }

    @NotNull
    private RTTIType<?> loadEnumType(@NotNull String name, @NotNull Map<String, Object> definition, boolean flags) {
        final List<Object> valuesInfo = getList(definition, "members");
        final int size = getInt(definition, "size");
        return new RTTITypeEnum(name, new RTTITypeEnum.Constant[valuesInfo.size()], size, flags);
    }

    @Nullable
    private RTTIType<?> loadPrimitiveType(@NotNull RTTITypeRegistry registry, @NotNull String name, @NotNull Map<String, Object> definition) {
        final String parent = getString(definition, version > 2 ? "base" : "parent_type");

        if (name.equals(parent)) {
            // Found an internal type, we can't load it here
            return null;
        }

        if (registry.find(parent, false) instanceof RTTITypePrimitive<?> p) {
            return p.clone(name);
        }

        return null;
    }

    private boolean isInstanceOf(@NotNull String type, @NotNull String base) {
        final Map<String, Object> declaration = declarations.get(type);
        final List<Map<String, Object>> bases = getList(declaration, "bases");

        if (type.equals(base)) {
            return true;
        }

        for (Map<String, Object> info : bases) {
            if (isInstanceOf(getString(info, "name"), base)) {
                return true;
            }
        }

        return false;
    }

    private void resolveClassType(@NotNull RTTITypeRegistry registry, @NotNull RTTITypeClass type, @NotNull Map<String, Object> definition) {
        final List<Map<String, Object>> basesInfo = getList(definition, "bases");
        final List<Map<String, Object>> membersInfo = getList(definition, "members");
        final List<String> messagesInfo = getList(definition, "messages");

        final var bases = new RTTITypeClass.MySuperclass[basesInfo.size()];
        final var fields = new ArrayList<MyField>(membersInfo.size());
        final var messages = new RTTIClass.Message[messagesInfo.size()];

        for (int i = 0; i < basesInfo.size(); i++) {
            final var baseInfo = basesInfo.get(i);
            final var baseType = (RTTITypeClass) registry.find(getString(baseInfo, "name"));
            final var baseOffset = getInt(baseInfo, "offset");

            bases[i] = new RTTITypeClass.MySuperclass(baseType, baseOffset);
        }

        for (final Map<String, Object> memberInfo : membersInfo) {
            final var memberType = registry.find(getString(memberInfo, "type"));
            final var memberName = getString(memberInfo, "name");
            final var memberCategory = memberInfo.containsKey("category") ? getString(memberInfo, "category") : "";
            final var memberOffset = getInt(memberInfo, "offset");
            final var memberFlags = getInt(memberInfo, "flags");

            fields.add(new MyField(type,
                memberType,
                memberName,
                memberCategory.isEmpty() ? null : memberCategory,
                memberOffset,
                memberFlags
            ));
        }

        for (int i = 0; i < messagesInfo.size(); i++) {
            final var name = messagesInfo.get(i);
            final var handlers = this.messages.get(type.getTypeName());
            final var handler = handlers != null ? handlers.get(name) : null;

            if (handler != null) {
                log.debug("Found message handler for type '{}' that handles message '{}'", type, name);
            }

            messages[i] = new MyMessage<>(handler, name);
        }

        type.setSuperclasses(bases);
        type.setFields(fields.toArray(MyField[]::new));
        type.setMessages(messages);

        final RTTIClass.Message<MessageHandler.ReadBinary> message = type.getDeclaredMessage("MsgReadBinary");
        final MessageHandler.ReadBinary handler = message != null ? message.getHandler() : null;

        if (message != null) {
            if (handler != null) {
                for (MessageHandler.ReadBinary.Component component : handler.components(registry)) {
                    fields.add(new MyField(type, component.type(), component.name(), null, Integer.MAX_VALUE, MyField.FLAG_NON_HASHABLE | MyField.FLAG_NON_READABLE));
                }
            } else {
                fields.add(new MyField(type, registry.find("Array<uint8>"), RTTITypeClass.EXTRA_DATA_FIELD, null, Integer.MAX_VALUE, MyField.FLAG_NON_HASHABLE | MyField.FLAG_NON_READABLE));
            }

            type.setFields(fields.toArray(MyField[]::new));
        }
    }

    private void resolveEnumType(@NotNull RTTITypeEnum type, @NotNull Map<String, Object> definition) {
        final List<Map<String, Object>> valuesInfo = getList(definition, "members");

        for (int i = 0; i < valuesInfo.size(); i++) {
            final var valueInfo = valuesInfo.get(i);
            final var valueName = getString(valueInfo, "name");
            final var valueData = getInt(valueInfo, "value");
            type.values()[i] = new RTTITypeEnum.MyConstant(type, valueName, valueData);
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

    public record MyMessage<T extends MessageHandler>(@Nullable T handler, @NotNull String name) implements RTTIClass.Message<T> {
        @Nullable
        @Override
        public T getHandler() {
            return handler;
        }

        @NotNull
        @Override
        public String getName() {
            return name;
        }
    }
}
