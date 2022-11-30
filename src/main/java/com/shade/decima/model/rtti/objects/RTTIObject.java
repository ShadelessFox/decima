package com.shade.decima.model.rtti.objects;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.util.NotNull;

import java.util.Map;

public record RTTIObject(@NotNull RTTITypeClass type, @NotNull Map<RTTIClass.Field<RTTIObject, ?>, Object> values) {
    @SuppressWarnings("unchecked")
    @NotNull
    public <T> T get(@NotNull RTTIClass.Field<RTTIObject, ?> member) {
        return (T) values.get(member);
    }

    @NotNull
    public <T> T get(@NotNull String name) {
        return this.<T>getField(name).get(this);
    }

    @NotNull
    public RTTIObject obj(@NotNull String name) {
        return get(name);
    }

    @NotNull
    public String str(@NotNull String name) {
        return get(name).toString();
    }

    @NotNull
    public RTTIReference ref(@NotNull String name) {
        return get(name);
    }

    public byte i8(@NotNull String name) {
        return get(name);
    }

    public short i16(@NotNull String name) {
        return get(name);
    }

    public int i32(@NotNull String name) {
        return get(name);
    }

    public long i64(@NotNull String name) {
        return get(name);
    }

    public float f32(@NotNull String name) {
        return get(name);
    }

    public double f64(@NotNull String name) {
        return get(name);
    }

    public boolean bool(@NotNull String name) {
        return get(name);
    }

    public void set(@NotNull RTTIClass.Field<RTTIObject, ?> field, @NotNull Object value) {
        values.put(field, value);
    }

    public void set(@NotNull String name, @NotNull Object value) {
        set(getField(name), value);
    }

    public void define(@NotNull String name, @NotNull RTTIType<?> type, @NotNull Object value) {
        for (RTTIClass.Field<RTTIObject, ?> field : values.keySet()) {
            if (field.getName().equals(name)) {
                throw new IllegalArgumentException("Duplicate field " + name);
            }
        }

        set(new RTTITypeClass.MyField(this.type, type, name, "", 0, 0), value);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private <T> RTTIClass.Field<RTTIObject, T> getField(@NotNull String name) {
        for (RTTIClass.Field<RTTIObject, ?> member : values.keySet()) {
            if (member.getName().equals(name)) {
                return (RTTIClass.Field<RTTIObject, T>) member;
            }
        }

        return type.getField(name);
    }
}
