package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

public class RTTITypeClass implements RTTIType<RTTIObject> {
    private final String name;
    private final RTTITypeClass[] bases;
    private final Field[] fields;

    public RTTITypeClass(@NotNull String name, @NotNull RTTITypeClass[] bases, @NotNull Field[] fields) {
        this.name = name;
        this.bases = bases;
        this.fields = fields;
    }

    @NotNull
    @Override
    public RTTIObject read(@NotNull ByteBuffer buffer) {
        final Map<String, Object> values = new LinkedHashMap<>();
        for (RTTITypeClass base : bases) {
            values.putAll(base.read(buffer).getFields());
        }
        for (Field field : fields) {
            values.put(field.name(), field.type().read(buffer));
        }
        return new RTTIObject(this, values);
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull RTTIObject value) {
        throw new IllegalStateException("Not implemented");
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public Class<RTTIObject> getType() {
        return RTTIObject.class;
    }

    @Override
    public int getSize() {
        int size = 0;
        for (RTTITypeClass base : bases) {
            size += base.getSize();
        }
        for (Field field : fields) {
            size += field.type().getSize();
        }
        return size;
    }

    public record Field(@NotNull String name, @NotNull RTTIType<?> type) {
    }
}
