package com.shade.decima.model.rtti.path;

import com.shade.util.NotNull;

import java.lang.reflect.Array;

public record PathElementIndex(int index) implements PathElement {
    @NotNull
    @Override
    public Object get(@NotNull Object object) {
        return Array.get(object, index);
    }

    @Override
    public void set(@NotNull Object object, @NotNull Object value) {
        Array.set(object, index, value);
    }
}
