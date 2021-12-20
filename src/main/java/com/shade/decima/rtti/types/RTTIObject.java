package com.shade.decima.rtti.types;

import com.shade.decima.util.NotNull;

import java.util.Map;

public class RTTIObject {
    private final RTTITypeClass type;
    private final Map<String, Object> fields;

    public RTTIObject(@NotNull RTTITypeClass type, @NotNull Map<String, Object> fields) {
        this.type = type;
        this.fields = fields;
    }

    @NotNull
    public RTTITypeClass getType() {
        return type;
    }

    @NotNull
    public Map<String, Object> getFields() {
        return fields;
    }

    @NotNull
    public Object getFieldValue(@NotNull String name) {
        final Object field = fields.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Class '" + type.getName() + "' does not have a field named '" + name + "'");
        }
        return field;
    }

    @Override
    public String toString() {
        return "RTTIObject[" + type.getName() + "] @ " + Integer.toHexString(hashCode());
    }
}
