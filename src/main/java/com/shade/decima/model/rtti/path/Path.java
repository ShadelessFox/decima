package com.shade.decima.model.rtti.path;

import com.shade.util.NotNull;

import java.util.Arrays;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Path path = (Path) o;
        return Arrays.equals(elements, path.elements);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(elements);
    }
}
