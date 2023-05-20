package com.shade.decima.model.rtti.path;

import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.util.Arrays;

public record RTTIPath(@NotNull RTTIPathElement... elements) {
    public RTTIPath {
        if (elements.length == 0) {
            throw new IllegalArgumentException("Path must consist of one or more elements");
        }
    }

    @NotNull
    public Object get(@NotNull Object object) {
        Object result = object;

        for (RTTIPathElement element : elements) {
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

    @NotNull
    public RTTIPath concat(@NotNull RTTIPathElement... other) {
        final RTTIPathElement[] result = new RTTIPathElement[elements.length + other.length];
        System.arraycopy(elements, 0, result, 0, elements.length);
        System.arraycopy(other, 0, result, elements.length, other.length);

        return new RTTIPath(result);
    }

    public boolean endsWith(@NotNull RTTIPathElement other) {
        return elements[elements.length - 1].equals(other);
    }

    public boolean startsWith(@NotNull RTTIPath other) {
        return IOUtils.startsWith(elements, other.elements);
    }

    @Override
    public String toString() {
        return "RTTIPath" + Arrays.toString(elements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RTTIPath path = (RTTIPath) o;
        return Arrays.equals(elements, path.elements);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(elements);
    }
}
