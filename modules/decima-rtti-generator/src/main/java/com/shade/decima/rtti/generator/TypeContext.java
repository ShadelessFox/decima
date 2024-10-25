package com.shade.decima.rtti.generator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shade.decima.rtti.TypeName;
import com.shade.decima.rtti.generator.data.*;
import com.shade.util.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

class TypeContext {
    private final Map<TypeName, TypeInfo> types = new TreeMap<>(Comparator.comparing(TypeName::fullName, String::compareToIgnoreCase));

    public void load(@NotNull Reader reader) throws IOException {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

        Resolver resolver = new Resolver() {
            private final Map<TypeName, FutureRef> pending = new HashMap<>();

            @NotNull
            @Override
            public TypeInfoRef get(@NotNull TypeName name) {
                if (pending.containsKey(name)) {
                    return pending.get(name);
                }

                TypeInfo type = types.get(name);
                if (type != null) {
                    return new ResolvedRef(type);
                }

                pending.put(name, new FutureRef(name));

                if (name instanceof TypeName.Parameterized parameterized) {
                    return resolve(getParameterizedType(parameterized.name(), parameterized.argument()));
                } else {
                    return resolve(getSimpleType(name.fullName()));
                }
            }

            @NotNull
            private TypeInfo getSimpleType(@NotNull String name) {
                JsonObject info = root.getAsJsonObject(name);
                if (info == null) {
                    throw new IllegalArgumentException("Unknown type: " + name);
                }
                return loadSingleType(name, info, this);
            }

            @NotNull
            private TypeInfo getParameterizedType(@NotNull String name, @NotNull TypeName argument) {
                JsonObject info = root.getAsJsonObject(name);
                if (info == null) {
                    throw new IllegalArgumentException("Unknown template type: " + name);
                }
                return loadParameterizedType(name, argument, info, this);
            }

            @NotNull
            private TypeInfoRef resolve(@NotNull TypeInfo info) {
                FutureRef ref = pending.remove(info.typeName());
                if (ref == null) {
                    throw new IllegalStateException("Type was not present in the queue: " + info.typeName());
                }
                if (types.put(info.typeName(), info) != null) {
                    throw new IllegalStateException("Type was already resolved: " + info.typeName());
                }
                ref.resolved = info;
                return ref;
            }
        };

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            var name = entry.getKey();
            if (name.startsWith("$")) {
                continue;
            }
            var object = entry.getValue().getAsJsonObject();
            var kind = object.get("kind").getAsString();
            if (kind.equals("pointer") || kind.equals("container")) {
                // These types are special because the dump doesn't contain
                // their specializations, so we can't resolve it here directly
                continue;
            }
            resolver.get(TypeName.of(name));
        }
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
            case "enum" -> loadEnumType(name, object, false);
            case "enum flags" -> loadEnumType(name, object, true);
            case "class" -> loadCompoundType(name, object, resolver);
            default -> throw new IllegalArgumentException("Unexpected kind of type: " + kind);
        };
    }

    @NotNull
    private TypeInfo loadParameterizedType(@NotNull String name, @NotNull TypeName argument, @NotNull JsonObject object, @NotNull Resolver resolver) {
        String kind = object.get("kind").getAsString();
        return switch (kind) {
            case "pointer" -> new PointerTypeInfo(name, resolver.get(argument));
            case "container" -> new ContainerTypeInfo(name, resolver.get(argument));
            default -> throw new IllegalArgumentException("Unexpected kind of type: " + kind);
        };
    }

    @NotNull
    private static TypeInfo loadAtomType(@NotNull String name, @NotNull JsonObject object, @NotNull Resolver resolver) {
        String base = object.get("base_type").getAsString();
        return new AtomTypeInfo(name, base.equals(name) ? null : resolver.get(TypeName.of(base)).value());
    }

    @NotNull
    private static TypeInfo loadEnumType(@NotNull String name, @NotNull JsonObject object, boolean flags) {
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
            flags
        );
    }

    @NotNull
    private static TypeInfo loadCompoundType(@NotNull String name, @NotNull JsonObject object, @NotNull Resolver resolver) {
        List<ClassBaseInfo> bases = new ArrayList<>();
        if (object.has("bases")) {
            for (JsonElement element : object.getAsJsonArray("bases")) {
                JsonObject base = element.getAsJsonObject();
                TypeName baseName = TypeName.of(base.get("name").getAsString());
                bases.add(new ClassBaseInfo(
                    resolver.get(baseName),
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
                    resolver.get(TypeName.parse(attr.get("type").getAsString())),
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
        TypeInfoRef get(@NotNull TypeName name);
    }

    private record ResolvedRef(@NotNull TypeInfo value) implements TypeInfoRef {
        @NotNull
        @Override
        public TypeName typeName() {
            return value.typeName();
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    private static class FutureRef implements TypeInfoRef {
        private final TypeName name;
        private TypeInfo resolved;

        public FutureRef(@NotNull TypeName name) {
            this.name = name;
        }

        @NotNull
        @Override
        public TypeName typeName() {
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
