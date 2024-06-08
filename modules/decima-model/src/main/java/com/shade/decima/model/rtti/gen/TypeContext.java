package com.shade.decima.model.rtti.gen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TypeContext {
    private static final Map<String, String> ALIASES = Map.ofEntries(
        // HZD
        Map.entry("GlobalRenderVariableInfo_GLOBAL_RENDER_VAR_COUNT", "Array"),
        Map.entry("ShaderProgramResourceSet_36", "Array"),
        Map.entry("float_GLOBAL_RENDER_VAR_COUNT", "Array"),
        Map.entry("uint16_PBD_MAX_SKIN_WEIGHTS", "Array"),
        Map.entry("uint64_PLACEMENT_LAYER_MASK_SIZE", "Array"),
        Map.entry("uint8_PBD_MAX_SKIN_WEIGHTS", "Array"),

        // HFW
        Map.entry("AmbientWaveInterpolatableSettings_AMBIENT_OCEAN_LOCATION_COUNT", "Array"),
        Map.entry("EnvelopeSegment_MAX_ENVELOPE_SEGMENTS", "Array"),
        Map.entry("ShaderProgramResourceSet_40", "Array"),
        Map.entry("Vec4_3", "Array"),
        Map.entry("WaveOctaveInterpolatableData_AMBIENT_OCEAN_LOCATION_COUNT", "Array"),
        Map.entry("float_WATER_SURFACE_MAX_SUPPORTED_SHADER_LODS_MAX_INDEX", "Array"),
        Map.entry("uint32_4", "Array"),
        Map.entry("uint32_5", "Array"),
        Map.entry("uint32_TEXTURE_STREAMING_MAX_MIPMAP_COUNT", "Array"),
        Map.entry("uint64_2", "Array")
    );

    private enum SpecVersion {
        VERSION_1,
        VERSION_5
    }

    private final Map<String, Type> types = new LinkedHashMap<>();
    private final Map<String, TypeRef<?>> pending = new HashMap<>();
    private SpecVersion specVersion = SpecVersion.VERSION_1;

    public void load(@NotNull Path source) throws IOException {
        try (Reader reader = Files.newBufferedReader(source)) {
            final JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                final String name = entry.getKey();
                if (name.equals("$spec")) {
                    throw new IllegalArgumentException("Parse spec");
                }
                processType(name, entry.getValue().getAsJsonObject());
            }
        }
    }

    @NotNull
    public Collection<Type> types() {
        return Collections.unmodifiableCollection(types.values());
    }

    private void processType(@NotNull String name, @NotNull JsonObject object) {
        final String kindName = switch (specVersion) {
            case VERSION_1 -> "type";
            case VERSION_5 -> "kind";
        };

        final String kind = object.get(kindName).getAsString();
        switch (kind) {
            case "class" -> processClass(name, object);
            case "enum" -> processEnum(name, object, false);
            case "enum flags" -> processEnum(name, object, true);
            case "primitive" -> processAtom(name, object);
            default -> throw new IllegalArgumentException("Unknown type: " + kind);
        }
    }

    private void processClass(@NotNull String name, @NotNull JsonObject object) {
        final String versionName = switch (specVersion) {
            case VERSION_1 -> "unknownC";
            case VERSION_5 -> "version";
        };
        final String attributesName = switch (specVersion) {
            case VERSION_1 -> "members";
            case VERSION_5 -> "attrs";
        };

        final int version = object.get(versionName).getAsInt();
        final int flags = object.get("flags").getAsInt();
        final ClassType type = new ClassType(name, version, flags);

        resolveType(type);

        if (object.has("bases")) {
            final JsonArray bases = object.get("bases").getAsJsonArray();
            for (int i = 0; i < bases.size(); i++) {
                final JsonObject base = bases.get(i).getAsJsonObject();
                type.bases.add(new ClassBase(
                    lookupType(base.get("name").getAsString()),
                    base.get("offset").getAsInt()
                ));
            }
        }

        if (object.has(attributesName)) {
            final JsonArray attrs = object.get(attributesName).getAsJsonArray();
            String category = null;

            for (int i = 0; i < attrs.size(); i++) {
                final JsonObject attr = attrs.get(i).getAsJsonObject();

                if (attr.has("category")) {
                    category = attr.get("category").getAsString();
                    if (category.isEmpty()) {
                        category = null;
                    }
                    if (specVersion == SpecVersion.VERSION_5) {
                        continue;
                    }
                }

                type.attrs.add(new ClassAttr(
                    lookupType(attr.get("type").getAsString()),
                    attr.get("name").getAsString(),
                    category,
                    attr.get("offset").getAsInt(),
                    attr.get("flags").getAsInt()
                ));
            }
        }
    }

    private void processEnum(@NotNull String name, @NotNull JsonObject object, boolean flags) {
        final String valuesName = switch (specVersion) {
            case VERSION_1 -> "members";
            case VERSION_5 -> "values";
        };

        final int size = object.get("size").getAsInt();
        final JsonArray values = object.get(valuesName).getAsJsonArray();
        final EnumType type = new EnumType(name, size, flags);

        resolveType(type);

        for (int i = 0; i < values.size(); i++) {
            final JsonObject value = values.get(i).getAsJsonObject();
            final List<String> aliases = new ArrayList<>();
            if (value.has("alias")) {
                final JsonArray array = value.get("alias").getAsJsonArray();
                for (int j = 0; j < array.size(); j++) {
                    aliases.add(array.get(j).getAsString());
                }
            }
            type.values.add(new EnumValue(
                value.get("name").getAsString(),
                aliases,
                value.get("value").getAsInt()
            ));
        }
    }

    private void processAtom(@NotNull String name, @NotNull JsonObject object) {
        final String baseName = switch (specVersion) {
            case VERSION_1 -> "parent_type";
            case VERSION_5 -> "base_type";
        };

        final String base = object.get(baseName).getAsString();

        if (base.equals(name)) {
            resolveType(new AtomType(name, null));
        } else {
            resolveType(new AtomType(name, lookupType(base)));
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Type> void resolveType(@NotNull T type) {
        final TypeRef<T> ref = (TypeRef<T>) pending.remove(type.name());
        if (ref != null) {
            ref.set(type);
        }
        if (types.putIfAbsent(type.name(), type) != null) {
            throw new IllegalStateException("Duplicate type: " + type.name());
        }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private <T extends Type> TypeRef<T> lookupType(@NotNull String name) {
        final String alias = ALIASES.get(name);
        if (alias != null) {
            return lookupType(alias);
        }
        final int index = name.indexOf('<');
        if (index >= 0) {
            final String template = name.substring(0, index);
            final String argument = name.substring(index + 1, name.length() - 1);
            return new TypeRef<>(name, (T) new TemplateType(template, lookupType(template), lookupType(argument)));
        }
        final T type = (T) types.get(name);
        if (type != null) {
            return new TypeRef<>(name, type);
        }
        return (TypeRef<T>) pending.computeIfAbsent(name, TypeRef::new);
    }

    public static abstract class Type {
        private final String name;

        public Type(@NotNull String name) {
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Type name cannot be empty");
            }
            this.name = name;
        }

        @NotNull
        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class TypeRef<T extends Type> {
        private final String name;
        private T type;

        public TypeRef(@NotNull String name) {
            this.name = name;
        }

        public TypeRef(@NotNull String name, @Nullable T type) {
            this.name = name;
            this.type = type;
        }

        @NotNull
        public String name() {
            return name;
        }

        @NotNull
        public T get() {
            return Objects.requireNonNull(type, "Type reference is not set for " + name);
        }

        public void set(@NotNull T type) {
            this.type = type;
        }
    }

    public record ClassBase(@NotNull TypeRef<ClassType> type, int offset) {
    }

    public record ClassAttr(@NotNull TypeRef<Type> type, @NotNull String name, @Nullable String category, int offset, int flags) {
    }

    public record EnumValue(@NotNull String name, @NotNull List<String> aliases, int value) {
    }

    public static class ClassType extends Type {
        private final List<ClassBase> bases = new ArrayList<>();
        private final List<ClassAttr> attrs = new ArrayList<>();
        private final int version;
        private final int flags;

        public ClassType(@NotNull String name, int version, int flags) {
            super(name);
            this.version = version;
            this.flags = flags;
        }

        @NotNull
        public List<ClassBase> bases() {
            return Collections.unmodifiableList(bases);
        }

        @NotNull
        public List<ClassAttr> attrs() {
            return Collections.unmodifiableList(attrs);
        }

        public int version() {
            return version;
        }

        public int flags() {
            return flags;
        }
    }

    public static class EnumType extends Type {
        private final List<EnumValue> values = new ArrayList<>();
        private final int size;
        private final boolean flags;

        public EnumType(@NotNull String name, int size, boolean flags) {
            super(name);
            this.size = size;
            this.flags = flags;
        }

        @NotNull
        public List<EnumValue> values() {
            return Collections.unmodifiableList(values);
        }

        public int size() {
            return size;
        }

        public boolean flags() {
            return flags;
        }
    }

    public static class AtomType extends Type {
        private final TypeRef<Type> base;

        public AtomType(@NotNull String name, @Nullable TypeRef<Type> base) {
            super(name);
            this.base = base;
        }

        @Nullable
        public TypeRef<Type> base() {
            return base;
        }
    }

    public static class TemplateType extends Type {
        private final TypeRef<Type> type;
        private final TypeRef<Type> argument;

        public TemplateType(@NotNull String name, @NotNull TypeRef<Type> type, @NotNull TypeRef<Type> argument) {
            super(name);
            this.type = type;
            this.argument = argument;
        }

        @NotNull
        public TypeRef<Type> type() {
            return type;
        }

        @NotNull
        public TypeRef<Type> argument() {
            return argument;
        }

        @Override
        public String toString() {
            return "%s<%s>".formatted(type.name(), argument.name());
        }
    }
}
