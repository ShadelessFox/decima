package com.shade.decima.rtti.data;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public sealed interface Value<T>
    permits Value.OfEnum, Value.OfEnumSet {

    non-sealed interface OfEnum<T extends Enum<T>> extends Value<T> {
    }

    non-sealed interface OfEnumSet<T extends Enum<T> & Value<T>> extends Value<T> {
        default boolean contains(T t) {
            return (value() & t.value()) == t.value();
        }
    }

    static <T extends Enum<T> & OfEnum<T>> OfEnum<T> valueOf(Class<T> enumClass, int value) {
        for (T constant : enumClass.getEnumConstants()) {
            if (constant.value() == value) {
                return constant;
            }
        }
        return new Value$OfConst<>(value);
    }

    static <T extends Enum<T> & OfEnumSet<T>> OfEnumSet<T> setOf(Class<T> enumClass, int value) {
        Set<Value<T>> values = new HashSet<>();
        for (T constant : enumClass.getEnumConstants()) {
            if ((constant.value() & value) != 0) {
                value &= ~constant.value();
                values.add(constant);
            }
        }
        if (value != 0) {
            values.add(new Value$OfConst<>(value));
        }
        return new Value$OfSet<>(Collections.unmodifiableSet(values));
    }

    int value();
}
