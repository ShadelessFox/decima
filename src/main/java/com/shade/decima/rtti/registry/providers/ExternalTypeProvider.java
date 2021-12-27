package com.shade.decima.rtti.registry.providers;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.registry.RTTITypeProvider;
import com.shade.decima.rtti.registry.RTTITypeRegistry;
import com.shade.decima.rtti.types.RTTITypeClass;
import com.shade.decima.rtti.types.RTTITypeEnum;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternalTypeProvider implements RTTITypeProvider {
    private static final Logger log = LoggerFactory.getLogger(ExternalTypeProvider.class);

    private final Map<String, Map<String, Object>> declarations = new HashMap<>();

    @Override
    public void initialize(@NotNull RTTITypeRegistry registry) {
        final String property = System.getProperty("decima.types.definition");
        if (property == null || !Files.exists(Path.of(property))) {
            log.warn("Property `decima.types.definition` is not set or points to an invalid file");
            return;
        }
        try (FileReader reader = new FileReader(property)) {
            declarations.putAll(new Yaml().load(reader));
        } catch (IOException e) {
            throw new RuntimeException("Error loading types definition from file " + property, e);
        }
    }

    @Nullable
    @Override
    public RTTIType<?> lookup(@NotNull RTTITypeRegistry registry, @NotNull String name) {
        final Map<String, Object> data = declarations.get(name);

        if (data == null) {
            return null;
        }

        return switch ((String) data.get("type")) {
            case "class" -> loadClassType(registry, data);
            case "enum" -> loadEnumType(registry, data);
            default -> throw new IllegalStateException("Unexpected type: " + data.get("type"));
        };
    }

    @NotNull
    private static RTTITypeClass loadClassType(@NotNull RTTITypeRegistry registry, @NotNull Map<String, Object> data) {
        final List<Map<String, Object>> bases = getList(data, "bases");
        final List<Map<String, Object>> members = getList(data, "members");
        members.removeIf(x -> Boolean.TRUE.equals(x.get("is_savestate")));

        final RTTITypeClass type = new RTTITypeClass(
            new RTTITypeClass[bases.size()],
            new RTTITypeClass.Field[members.size()]
        );

        for (int i = 0; i < bases.size(); i++) {
            type.getBases()[i] = (RTTITypeClass) (RTTIType<?>) registry.get((String) bases.get(i).get("name"));
        }

        for (int i = 0; i < members.size(); i++) {
            final Map<String, Object> member = members.get(i);

            type.getFields()[i] = new RTTITypeClass.Field(
                type,
                (String) member.get("name"),
                registry.get((String) member.get("type"))
            );
        }

        return type;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static RTTITypeEnum<?> loadEnumType(@NotNull RTTITypeRegistry registry, @NotNull Map<String, Object> data) {
        final int size = (Integer) data.get("size");
        final List<List<Object>> values = ExternalTypeProvider.getList(data, "values");

        final RTTITypeEnum<Number> type = new RTTITypeEnum<>(
            getComponentType(registry, size),
            (RTTITypeEnum.Constant<Number>[]) new RTTITypeEnum.Constant<?>[values.size()]
        );

        for (int i = 0; i < values.size(); i++) {
            final String name = (String) values.get(i).get(0);
            final Number value = getComponentValue((Number) values.get(i).get(1), size);
            type.getConstants()[i] = new RTTITypeEnum.Constant<>(type, name, value);
        }

        return type;
    }

    @NotNull
    private static RTTIType<Number> getComponentType(@NotNull RTTITypeRegistry registry, int size) {
        return switch (size) {
            case 1 -> registry.get("uint8");
            case 2 -> registry.get("uint16");
            case 4 -> registry.get("uint32");
            default -> throw new IllegalStateException("Unexpected enum component size: " + size);
        };
    }

    @NotNull
    private static Number getComponentValue(@NotNull Number value, int size) {
        return switch (size) {
            case 1 -> value.byteValue();
            case 2 -> value.shortValue();
            case 4 -> value.intValue();
            default -> throw new IllegalStateException("Unexpected enum component size: " + size);
        };
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static <T> List<T> getList(@NotNull Map<String, ?> map, @NotNull String key) {
        final List<T> data = (List<T>) map.get(key);
        return data != null ? data : Collections.emptyList();
    }
}
