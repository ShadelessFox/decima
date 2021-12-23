package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.objects.RTTIObject;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

public class RTTITypeClass implements RTTIType<RTTIObject> {
    private final RTTITypeClass[] bases;
    private final Field[] fields;

    public RTTITypeClass(@NotNull RTTITypeClass[] bases, @NotNull Field[] fields) {
        this.bases = bases;
        this.fields = fields;
    }

    @NotNull
    @Override
    public RTTIObject read(@NotNull ByteBuffer buffer) {
        final Map<RTTITypeClass.Field, Object> values = new LinkedHashMap<>();
        for (RTTITypeClass base : bases) {
            values.putAll(base.read(buffer).getFields());
        }
        for (Field field : fields) {
            values.put(field, field.type().read(buffer));
        }
        return new RTTIObject(this, values);
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull RTTIObject value) {
        for (RTTITypeClass base : bases) {
            base.write(buffer, value);
        }
        for (Field field : fields) {
            field.type().write(buffer, value.getFieldValue(field));
        }
    }

    @NotNull
    @Override
    public Class<RTTIObject> getType() {
        return RTTIObject.class;
    }

    @NotNull
    public RTTITypeClass[] getBases() {
        return bases;
    }

    @NotNull
    public Field[] getFields() {
        return fields;
    }

    public record Field(@NotNull RTTITypeClass parent, @NotNull String name, @NotNull RTTIType<?> type) {
    }
}
