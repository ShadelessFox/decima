package com.shade.decima.rtti;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public sealed interface Value<T> {
    static <T extends Enum<T> & Value.OfEnum<T>> Value<T> valueOf(Class<T> enumClass, int value) {
        for (T constant : enumClass.getEnumConstants()) {
            if (constant.value() == value) {
                return constant;
            }
        }
        return new OfConst<>(value);
    }

    static <T extends Enum<T> & Value.OfEnumSet<T>> Set<Value<T>> setOf(Class<T> enumClass, int value) {
        Set<Value<T>> values = new HashSet<>();
        for (T constant : enumClass.getEnumConstants()) {
            if ((constant.value() & value) != 0) {
                value &= ~constant.value();
                values.add(constant);
            }
        }
        if (value != 0) {
            values.add(new OfConst<>(value));
        }
        return Collections.unmodifiableSet(values);
    }

    int value();

    non-sealed interface OfEnum<E extends Enum<E>> extends Value<E> {
    }

    non-sealed interface OfEnumSet<E extends Enum<E>> extends Value<E> {
    }

    record OfConst<T>(int value) implements Value<T> {
        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}