package com.shade.decima.model.rtti.registry.providers;

import com.google.gson.*;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.registry.RTTITypeProvider;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class ExternalTypeProvider implements RTTITypeProvider {
    private static final Logger log = LoggerFactory.getLogger(ExternalTypeProvider.class);

    private final Map<String, Map<String, MessageHandler>> messages = new HashMap<>();

    private JsonObject root;
    private Version version = new Version(1, 0);

    @Override
    public void initialize(@NotNull RTTIFactory factory, @NotNull ProjectContainer container) throws IOException {
        try (Reader reader = container.getTypeMetadata()) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        if (root.has("$spec")) {
            JsonObject spec = root.remove("$spec").getAsJsonObject();
            JsonPrimitive version = spec.getAsJsonPrimitive("version");
            if (version.isNumber()) {
                this.version = new Version(version.getAsInt(), 0);
            } else {
                this.version = Version.fromString(version.getAsString());
            }
        }

        for (String type : root.keySet()) {
            if (lookup(factory, type) instanceof RTTIClass cls && isInstanceOf(type, "RTTIRefObject")) {
                factory.define(this, cls);
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
    public RTTIType<?> lookup(@NotNull RTTIFactory factory, @NotNull String name) {
        final JsonObject definition = root.getAsJsonObject(name);

        if (definition == null) {
            return null;
        }

        return switch (definition.get(version.atLeast(4) ? "kind" : "type").getAsString()) {
            case "class" -> loadClassType(name, definition);
            case "enum" -> loadEnumType(name, definition, false);
            case "enum flags" -> loadEnumType(name, definition, true);
            case "primitive" -> loadPrimitiveType(factory, name, definition);
            case "container", "reference" -> null;
            default -> throw new IllegalStateException("Unsupported type '" + definition.get("type") + "'");
        };
    }

    @Override
    public void resolve(@NotNull RTTIFactory factory, @NotNull RTTIType<?> type) {
        final JsonObject definition = root.getAsJsonObject(type.getFullTypeName());

        switch (definition.get(version.atLeast(4) ? "kind" : "type").getAsString()) {
            case "class" -> resolveClassType(factory, (RTTITypeClass) type, definition);
            case "enum", "enum flags" -> resolveEnumType((RTTITypeEnum) type, definition);
            case "primitive" -> { /* nothing to resolve */ }
            default -> throw new IllegalStateException("Unsupported type '" + definition.get("type") + "'");
        }
    }

    @NotNull
    private RTTITypeClass loadClassType(@NotNull String name, @NotNull JsonObject definition) {
        return new RTTITypeClass(
            name,
            definition.get(version.atLeast(3) ? "version" : version.atLeast(2) ? "flags1" : "unknownC").getAsInt(),
            definition.get(version.atLeast(3) ? "flags" : version.atLeast(2) ? "flags2" : "flags").getAsInt()
        );
    }

    @NotNull
    private RTTIType<?> loadEnumType(@NotNull String name, @NotNull JsonObject definition, boolean flags) {
        var valuesInfo = getArray(definition, version.atLeast(4) ? "values" : "members");
        var size = definition.get("size").getAsInt();
        return new RTTITypeEnum(name, new RTTITypeEnum.Constant[valuesInfo.size()], size, flags);
    }

    @Nullable
    private RTTIType<?> loadPrimitiveType(@NotNull RTTIFactory factory, @NotNull String name, @NotNull JsonObject definition) {
        var parent = definition.get(version.atLeast(4) ? "base_type" : version.atLeast(3) ? "base" : "parent_type").getAsString();

        if (name.equals(parent)) {
            // Found an internal type, we can't load it here
            return null;
        }

        if (factory.find(parent, false) instanceof RTTITypePrimitive<?> p) {
            return p.clone(name);
        }

        return null;
    }

    private boolean isInstanceOf(@NotNull String type, @NotNull String base) {
        var declaration = root.getAsJsonObject(type);
        var bases = getArray(declaration, "bases");

        if (type.equals(base)) {
            return true;
        }

        for (JsonElement info : bases) {
            if (isInstanceOf(info.getAsJsonObject().get("name").getAsString(), base)) {
                return true;
            }
        }

        return false;
    }

    private void resolveClassType(@NotNull RTTIFactory factory, @NotNull RTTITypeClass type, @NotNull JsonObject definition) {
        var basesInfo = getArray(definition, "bases");
        var attrsInfo = getArray(definition, version.atLeast(4) ? "attrs" : "members");
        var messagesInfo = getArray(definition, "messages");

        var bases = new RTTITypeClass.MySuperclass[basesInfo.size()];
        var fields = new ArrayList<MyField>(attrsInfo.size());
        var messages = new RTTIClass.Message[messagesInfo.size()];

        for (int i = 0; i < basesInfo.size(); i++) {
            var baseInfo = basesInfo.get(i).getAsJsonObject();
            var baseType = factory.<RTTITypeClass>find(baseInfo.get("name").getAsString());
            var baseOffset = baseInfo.get("offset").getAsInt();

            bases[i] = new RTTITypeClass.MySuperclass(baseType, baseOffset);
        }

        String category = null;

        for (int i = 0; i < attrsInfo.size(); i++) {
            var memberInfo = attrsInfo.get(i).getAsJsonObject();
            var memberCategory = Optional.ofNullable(memberInfo.get("category"))
                .map(JsonElement::getAsString)
                .filter(Predicate.not(String::isEmpty))
                .orElse(null);

            if (!version.atLeast(5, 0)) {
                category = memberCategory;
            } else if (memberCategory != null) {
                category = memberCategory;
                continue;
            }

            var memberType = factory.find(memberInfo.get("type").getAsString());
            var memberName = memberInfo.get("name").getAsString();
            var memberOffset = memberInfo.get("offset").getAsInt();
            var memberFlags = memberInfo.get("flags").getAsInt();

            fields.add(new MyField(
                type,
                memberType,
                memberName,
                category,
                memberOffset,
                memberFlags
            ));
        }

        for (int i = 0; i < messagesInfo.size(); i++) {
            var name = messagesInfo.get(i).getAsString();
            var handlers = this.messages.get(type.getTypeName());
            var handler = handlers != null ? handlers.get(name) : null;

            if (handler != null) {
                log.debug("Found message handler for type '{}' that handles message '{}'", type, name);
            }

            messages[i] = new MyMessage<>(handler, name);
        }

        type.setSuperclasses(bases);
        type.setFields(fields.toArray(MyField[]::new));
        type.setMessages(messages);

        var message = type.<MessageHandler.ReadBinary>getDeclaredMessage("MsgReadBinary");
        var handler = message != null ? message.getHandler() : null;

        if (message != null) {
            if (handler != null) {
                for (MessageHandler.ReadBinary.Component component : handler.components(factory)) {
                    fields.add(new MyField(type, component.type(), component.name(), null, Integer.MAX_VALUE, MyField.FLAG_NON_HASHABLE | MyField.FLAG_NON_READABLE));
                }
            } else {
                fields.add(new MyField(type, factory.find("Array<uint8>"), RTTITypeClass.EXTRA_DATA_FIELD, null, Integer.MAX_VALUE, MyField.FLAG_NON_HASHABLE | MyField.FLAG_NON_READABLE));
            }

            type.setFields(fields.toArray(MyField[]::new));
        }
    }

    private void resolveEnumType(@NotNull RTTITypeEnum type, @NotNull JsonObject definition) {
        final JsonArray valuesInfo = getArray(definition, version.atLeast(4) ? "values" : "members");

        for (int i = 0; i < valuesInfo.size(); i++) {
            var valueInfo = valuesInfo.get(i);
            var valueName = ((JsonObject) valueInfo).get("name").getAsString();
            var valueData = ((JsonObject) valueInfo).get("value").getAsInt();

            type.values()[i] = new RTTITypeEnum.MyConstant(type, valueName, valueData);
        }
    }

    @NotNull
    private static JsonArray getArray(@NotNull JsonObject map, @NotNull String key) {
        if (map.has(key)) {
            return map.getAsJsonArray(key);
        } else {
            return new JsonArray();
        }
    }

    private record MyMessage<T extends MessageHandler>(@Nullable T handler, @NotNull String name) implements RTTIClass.Message<T> {
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

    private record Version(int major, int minor) {
        @NotNull
        public static Version fromString(@NotNull String version) {
            String[] parts = version.split("\\.");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid version format: " + version);
            }
            return new Version(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }

        public boolean atLeast(int major, int minor) {
            return this.major > major || this.major == major && this.minor >= minor;
        }

        public boolean atLeast(int major) {
            return this.major >= major;
        }
    }
}
