package com.shade.decima.rtti.data;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

record Value$OfSet<T extends Enum<T> & Value.OfEnumSet<T>>(Set<Value<T>> values) implements Value.OfEnumSet<T> {
    @Override
    public boolean contains(T t) {
        return values.contains(t);
    }

    @Override
    public int value() {
        return values.stream().mapToInt(Value::value).reduce(0, (a, b) -> a | b);
    }

    @Override
    public String toString() {
        return values.stream().map(Objects::toString).collect(Collectors.joining("|"));
    }
}
