package com.shade.decima.rtti.data;

import java.util.Iterator;

public interface Ref<T> {
    T get();

    static <T> Iterable<T> unwrap(Iterable<Ref<T>> iterable) {
        return () -> unwrap(iterable.iterator());
    }

    static <T> Iterator<T> unwrap(Iterator<Ref<T>> iterator) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next().get();
            }
        };
    }
}
