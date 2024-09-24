package com.shade.decima.rtti.generator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shade.decima.rtti.generator.data.*;
import com.shade.util.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TypeContext {
    private final Map<String, TypeInfo> types = new TreeMap<>(String::compareToIgnoreCase);

    public void load(@NotNull Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            Resolver resolver = new Resolver() {
                private final Map<String, FutureRef> pending = new HashMap<>();

                @NotNull
                @Override
                public TypeInfoRef get(@NotNull String name) {
                    if (pending.containsKey(name)) {
                        return pending.get(name);
                    }

                    TypeInfo type = types.get(name);
                    if (type != null) {
                        return new ResolvedRef(type);
                    }

                    pending.put(name, new FutureRef(name));

                    if (name.indexOf('<') >= 0) {
                        int templateStart = name.indexOf('<');
                        int templateEnd = name.lastIndexOf('>');
                        if (templateEnd < templateStart + 1) {
                            throw new IllegalArgumentException("Invalid template name: '" + name + "'");
                        }

                        String rawType = name.substring(0, templateStart);
                        String argumentType = name.substring(templateStart + 1, templateEnd);

                        return resolve(get(rawType, argumentType));
                    } else {
                        JsonObject info = root.getAsJsonObject(name);
                        if (info == null) {
                            throw new IllegalArgumentException("Unknown type: " + name);
                        }

                        return resolve(loadSingleType(name, info, this));
                    }
                }

                @NotNull
                @Override
                public TypeInfo get(@NotNull String name, @NotNull String argument) {
                    return switch (name) {
                        case "Array", "TinyArray" -> new ContainerTypeInfo(name, get(argument));
                        case "Ref", "cptr" -> new PointerTypeInfo(name, get(argument));
                        default -> throw new IllegalArgumentException("Unknown template type: " + name);
                    };
                }

                @NotNull
                private TypeInfoRef resolve(@NotNull TypeInfo info) {
                    FutureRef ref = pending.remove(info.fullName());
                    if (ref == null) {
                        throw new IllegalStateException("Type was not present in the queue: " + info.fullName());
                    }
                    if (types.put(info.fullName(), info) != null) {
                        throw new IllegalStateException("Type was already resolved: " + info.fullName());
                    }
                    ref.resolved = info;
                    return ref;
                }
            };

            for (String name : root.keySet()) {
                if (name.startsWith("$")) {
                    continue;
                }
                resolver.get(name);
            }
        }
    }

    @NotNull
    public TypeInfo get(@NotNull String name) {
        return Objects.requireNonNull(types.get(name), () -> "Unknown type: " + name);
    }

    @NotNull
    public Collection<TypeInfo> types() {
        return Collections.unmodifiableCollection(types.values());
    }

    @NotNull
    private static TypeInfo loadSingleType(@NotNull String name, @NotNull JsonObject object, @NotNull Resolver resolver) {
        String kind = object.get("kind").getAsString();
        return switch (kind) {
            case "primitive" -> loadAtomType(name, object, resolver);
            case "enum" -> loadEnumType(name, object, resolver);
            case "class" -> loadCompoundType(name, object, resolver);
            default -> throw new IllegalArgumentException("Unexpected kind of type: " + kind);
        };
    }

    @NotNull
    private static TypeInfo loadAtomType(@NotNull String name, @NotNull JsonObject object, @NotNull Resolver resolver) {
        String base = object.get("base_type").getAsString();
        return new AtomTypeInfo(name, base.equals(name) ? null : resolver.get(base).value());
    }

    @NotNull
    private static TypeInfo loadEnumType(@NotNull String name, @NotNull JsonObject object, @NotNull Resolver resolver) {
        List<EnumValueInfo> values = new ArrayList<>();
        for (JsonElement element : object.getAsJsonArray("values")) {
            JsonObject value = element.getAsJsonObject();
            values.add(new EnumValueInfo(
                value.get("name").getAsString(),
                value.get("value").getAsInt()
            ));
        }

        EnumValueSize size = switch (object.get("size").getAsInt()) {
            case 1 -> EnumValueSize.INT8;
            case 2 -> EnumValueSize.INT16;
            case 4 -> EnumValueSize.INT32;
            default -> throw new IllegalArgumentException("Invalid enum size: " + object.get("size").getAsInt());
        };

        return new EnumTypeInfo(
            name,
            values,
            size,
            object.get("kind").getAsString().equals("enum flags")
        );
    }

    @NotNull
    private static TypeInfo loadCompoundType(@NotNull String name, @NotNull JsonObject object, @NotNull Resolver resolver) {
        List<ClassBaseInfo> bases = new ArrayList<>();
        if (object.has("bases")) {
            for (JsonElement element : object.getAsJsonArray("bases")) {
                JsonObject base = element.getAsJsonObject();
                bases.add(new ClassBaseInfo(
                    (ClassTypeInfo) resolver.get(base.get("name").getAsString()).value(),
                    base.get("offset").getAsInt()
                ));
            }
        }

        List<ClassAttrInfo> attrs = new ArrayList<>();
        if (object.has("attrs")) {
            String category = null;
            for (JsonElement element : object.getAsJsonArray("attrs")) {
                JsonObject attr = element.getAsJsonObject();
                if (attr.has("category")) {
                    category = attr.get("category").getAsString();
                    continue;
                }
                attrs.add(new ClassAttrInfo(
                    attr.get("name").getAsString(),
                    category,
                    resolver.get(attr.get("type").getAsString()),
                    attr.has("min") ? attr.get("min").getAsString() : null,
                    attr.has("max") ? attr.get("max").getAsString() : null,
                    attrs.size(),
                    attr.get("offset").getAsInt(),
                    attr.get("flags").getAsInt(),
                    attr.has("property")
                ));
            }
        }

        Set<String> messages = new HashSet<>();
        if (object.has("messages")) {
            for (JsonElement element : object.getAsJsonArray("messages")) {
                messages.add(element.getAsString());
            }
        }

        return new ClassTypeInfo(
            name,
            bases,
            attrs,
            messages,
            object.has("version") ? object.get("version").getAsInt() : 0,
            object.has("flags") ? object.get("flags").getAsInt() : 0
        );
    }

    private interface Resolver {
        @NotNull
        TypeInfoRef get(@NotNull String name);

        @NotNull
        TypeInfo get(@NotNull String name, @NotNull String argument);
    }

    private record ResolvedRef(@NotNull TypeInfo value) implements TypeInfoRef {
        @NotNull
        @Override
        public String typeName() {
            return value.fullName();
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    private static class FutureRef implements TypeInfoRef {
        private final String name;
        private TypeInfo resolved;

        public FutureRef(@NotNull String name) {
            this.name = name;
        }

        @NotNull
        @Override
        public String typeName() {
            return name;
        }

        @NotNull
        @Override
        public TypeInfo value() {
            if (resolved == null) {
                throw new IllegalStateException("Type '" + name + "' is not resolved");
            }
            return resolved;
        }

        @Override
        public String toString() {
            return resolved != null ? resolved.toString() : "<pending>";
        }
    }
}
