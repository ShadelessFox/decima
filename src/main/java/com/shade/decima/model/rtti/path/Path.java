package com.shade.decima.model.rtti.path;

import com.shade.util.NotNull;

public record Path(@NotNull PathElement[] elements) {
    public Path {
        if (elements.length == 0) {
            throw new IllegalArgumentException("Path must consist of one or more elements");
        }
    }

    @NotNull
    public Object get(@NotNull Object object) {
        Object result = object;

        for (PathElement element : elements) {
            result = element.get(result);
        }

        return result;
    }

    public void set(@NotNull Object object, @NotNull Object value) {
        Object current = object;

        for (int i = 0; i < elements.length - 1; i++) {
            current = elements[i].get(current);
        }

        elements[elements.length - 1].set(current, value);
    }
}
