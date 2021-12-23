package com.shade.decima.rtti;

import com.shade.decima.rtti.types.RTTITypeClass;
import com.shade.decima.rtti.types.RTTITypeEnum;
import com.shade.decima.util.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RTTILoader {
    private final Map<String, Map<String, Object>> registry;

    public RTTILoader(@NotNull FileReader registry) {
        this.registry = new Yaml().load(registry);
    }

    @NotNull
    public RTTIType<?> find(@NotNull String name) {
        return RTTITypeRegistry.get(name, this::find0);
    }

    @NotNull
    private RTTIType<?> find0(@NotNull String name) {
        final Map<String, Object> data = registry.get(name);

        if (data == null) {
            throw new IllegalArgumentException("Can't find type named '" + name + "'");
        }

        return switch ((String) data.get("type")) {
            case "class" -> loadClassType(name, data);
            case "enum" -> loadEnumType(name, data);
            default -> throw new IllegalStateException("Unexpected type: " + data.get("type"));
        };
    }

    @NotNull
    private RTTITypeClass loadClassType(@NotNull String name, @NotNull Map<String, Object> data) {
        final List<Map<String, Object>> bases = RTTILoader.getList(data, "bases");
        final List<Map<String, Object>> members = RTTILoader.getList(data, "members");

        final RTTITypeClass type = new RTTITypeClass(
            new RTTITypeClass[bases.size()],
            new RTTITypeClass.Field[members.size()]
        );

        for (int i = 0; i < bases.size(); i++) {
            type.getBases()[i] = (RTTITypeClass) find((String) bases.get(i).get("name"));
        }

        for (int i = 0; i < members.size(); i++) {
            final Map<String, Object> member = members.get(i);

            if (Boolean.TRUE.equals(member.get("is_savestate"))) {
                continue;
            }

            type.getFields()[i] = new RTTITypeClass.Field(
                type,
                (String) member.get("name"),
                find((String) member.get("type"))
            );
        }

        RTTITypeRegistry.register(type, name);

        return type;
    }

    @NotNull
    private RTTITypeEnum<?> loadEnumType(@NotNull String name, @NotNull Map<String, Object> data) {
        final RTTIType<Number> componentType = switch ((Integer) data.get("size")) {
            case 1 -> RTTITypeRegistry.get("uint8");
            case 2 -> RTTITypeRegistry.get("uint16");
            case 4 -> RTTITypeRegistry.get("uint32");
            default -> throw new IllegalStateException("Unexpected enum component size: " + data.get("size"));
        };

        final Map<Number, String> values = new HashMap<>();

        for (List<Object> value : RTTILoader.<List<Object>>getList(data, "values")) {
            values.put((Number) value.get(1), (String) value.get(0));
        }

        final RTTITypeEnum<Number> type = new RTTITypeEnum<>(
            componentType, values
        );

        RTTITypeRegistry.register(type, name);

        return type;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> getList(@NotNull Map<String, ?> map, @NotNull String key) {
        final List<T> data = (List<T>) map.get(key);
        return data != null ? data : Collections.emptyList();
    }
}
