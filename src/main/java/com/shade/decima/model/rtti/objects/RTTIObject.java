package com.shade.decima.model.rtti.objects;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.util.NotNull;

import java.util.Iterator;

/**
 * A lightweight wrapper around an implementation-specific RTTI object that provides various accessors.
 */
public interface RTTIObject extends Iterable<RTTIClass.Field<?>> {
    @NotNull
    <T> T get(@NotNull RTTIClass.Field<?> field);

    @NotNull
    <T> T get(@NotNull String name);

    void set(@NotNull RTTIClass.Field<?> field, @NotNull Object value);

    void set(@NotNull String name, @NotNull Object value);

    void define(@NotNull String name, @NotNull RTTIType<?> type, @NotNull Object value);

    default void define(@NotNull String name, @NotNull RTTIObject object) {
        define(name, object.type(), object);
    }

    @NotNull
    RTTIClass type();

    // FIXME: The Iterable is a hack for accessing extra fields declared in _dynamic_ way.
    //        Once we figure out how to not dynamically extend existing classes or, rather,
    //        their instances, this can be removed.
    @NotNull
    @Override
    Iterator<RTTIClass.Field<?>> iterator();

    @NotNull
    default RTTIObject obj(@NotNull String name) {
        return get(name);
    }

    @NotNull
    default String str(@NotNull String name) {
        return get(name).toString();
    }

    @NotNull
    default RTTIReference ref(@NotNull String name) {
        return get(name);
    }

    default byte i8(@NotNull String name) {
        return get(name);
    }

    default short i16(@NotNull String name) {
        return get(name);
    }

    default int i32(@NotNull String name) {
        return get(name);
    }

    default long i64(@NotNull String name) {
        return get(name);
    }

    default float f32(@NotNull String name) {
        return get(name);
    }

    default double f64(@NotNull String name) {
        return get(name);
    }

    default boolean bool(@NotNull String name) {
        return get(name);
    }
}
