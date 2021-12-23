package com.shade.decima.rtti.objects;

import com.shade.decima.rtti.types.RTTITypeClass;
import com.shade.decima.util.NotNull;

import java.util.Map;

public class RTTIObject {
    private final RTTITypeClass type;
    private final Map<RTTITypeClass.Field, Object> fields;

    public RTTIObject(@NotNull RTTITypeClass type, @NotNull Map<RTTITypeClass.Field, Object> fields) {
        this.type = type;
        this.fields = fields;
    }

    @NotNull
    public RTTITypeClass getType() {
        return type;
    }

    @NotNull
    public Map<RTTITypeClass.Field, Object> getFields() {
        return fields;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T> T getFieldValue(@NotNull RTTITypeClass.Field field) {
        final Object value = fields.get(field);
        if (value == null) {
            throw new IllegalArgumentException("Object of type '" + type + "' does not have a field named '" + field + "'");
        }
        return (T) value;
    }
}
