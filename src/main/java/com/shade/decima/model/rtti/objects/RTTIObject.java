package com.shade.decima.model.rtti.objects;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.util.NotNull;

/**
 * A lightweight wrapper around an implementation-specific RTTI object that provides various accessors.
 */
public record RTTIObject(@NotNull RTTIClass type, @NotNull Object data) {
    @SuppressWarnings("unchecked")
    public <T> T cast() {
        return (T) data;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(@NotNull RTTIClass.Field<?> field) {
        return (T) field.get(this);
    }

    public <T> T get(@NotNull String name) {
        return type().<T>getField(name).get(this);
    }

    @SuppressWarnings("unchecked")
    public void set(@NotNull RTTIClass.Field<?> field, @NotNull Object value) {
        ((RTTIClass.Field<Object>) field).set(this, value);
    }

    public void set(@NotNull String name, @NotNull Object value) {
        type().getField(name).set(this, value);
    }

    @NotNull
    public RTTIObject obj(@NotNull String name) {
        return get(name);
    }

    @NotNull
    public RTTIObject[] objs(@NotNull String name) {
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
}
