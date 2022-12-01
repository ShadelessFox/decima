package com.shade.decima.model.rtti.types.java;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.util.NotNull;

import java.util.Iterator;
import java.util.List;

public record JavaObject(@NotNull RTTIClass type, @NotNull Object object) implements RTTIObject {
    @NotNull
    @Override
    public <T> T get(@NotNull RTTIClass.Field<?> field) {
        return null;
    }

    @NotNull
    @Override
    public <T> T get(@NotNull String name) {
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T into() {
        return (T) object;
    }

    @Override
    public void set(@NotNull RTTIClass.Field<?> field, @NotNull Object value) {

    }

    @Override
    public void set(@NotNull String name, @NotNull Object value) {

    }

    @Override
    public void define(@NotNull String name, @NotNull RTTIType<?> type, @NotNull Object value) {

    }

    @NotNull
    @Override
    public RTTIClass type() {
        return type;
    }

    @NotNull
    @Override
    public Iterator<RTTIClass.Field<?>> iterator() {
        return List.of(type.getFields()).iterator();
    }
}
