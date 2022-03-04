package com.shade.decima.model.rtti.objects;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class RTTICollection<T> implements Iterable<T> {
    private final RTTIType<T> type;
    private final T[] data;

    public RTTICollection(@NotNull RTTIType<T> type, @NotNull T[] data) {
        this.type = type;
        this.data = data;
    }

    @NotNull
    public T get(int index) {
        return data[index];
    }

    public int size() {
        return data.length;
    }

    @NotNull
    public RTTIType<T> getType() {
        return type;
    }

    @NotNull
    public T[] toArray() {
        return data;
    }

    @NotNull
    public Iterator<T> iterator() {
        return new ArrayIterator<>(data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RTTICollection<?> that = (RTTICollection<?>) o;
        return type.equals(that.type) && Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        return "RTTICollection{type=" + type + ", data=" + Arrays.toString(data) + '}';
    }

    private static class ArrayIterator<T> implements Iterator<T> {
        private final T[] array;
        private int cursor;

        public ArrayIterator(@NotNull T[] array) {
            this.array = array;
            this.cursor = 0;
        }

        @Override
        public boolean hasNext() {
            return cursor < array.length;
        }

        @Override
        public T next() {
            if (cursor >= array.length) {
                throw new NoSuchElementException();
            }
            return array[cursor++];
        }
    }
}
