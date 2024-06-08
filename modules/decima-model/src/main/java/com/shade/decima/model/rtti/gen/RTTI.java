package com.shade.decima.model.rtti.gen;

import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.util.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RTTI {
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Serializable {
        long hash();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Attr {
        String type() default "";

        String category() default "";

        int offset();

        int flags();
    }

    @Target({ElementType.TYPE_USE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Base {
        int offset();
    }

    public sealed interface Enum {
        String name();

        String[] aliases();

        non-sealed interface OfByte extends Enum {
            byte value();
        }

        non-sealed interface OfShort extends Enum {
            short value();
        }

        non-sealed interface OfInt extends Enum {
            int value();
        }
    }

    @NotNull
    public static List<AttrWithOffset> collectAttrs(@NotNull Class<?> type) {
        final List<AttrWithOffset> fields = new ArrayList<>();
        collectAttrs(type, fields, 0);
        filterAttrs(fields);
        sortAttrs(fields);
        return fields;
    }

    private static void collectAttrs(@NotNull Class<?> cls, @NotNull List<AttrWithOffset> result, int offset) {
        for (AnnotatedType type : cls.getAnnotatedInterfaces()) {
            final RTTI.Base base = type.getDeclaredAnnotation(RTTI.Base.class);
            collectAttrs((Class<?>) type.getType(), result, base.offset() + offset);
        }
        for (Method method : cls.getDeclaredMethods()) {
            final RTTI.Attr attr = method.getDeclaredAnnotation(RTTI.Attr.class);
            result.add(new AttrWithOffset(method, attr, attr.offset() + offset));
        }
    }

    private static void filterAttrs(@NotNull List<AttrWithOffset> attrs) {
        attrs.removeIf(attr -> (attr.attr().flags() & 2) != 0);
    }

    private static void sortAttrs(@NotNull List<AttrWithOffset> attrs) {
        RTTIUtils.quickSort(attrs, Comparator.comparingInt(AttrWithOffset::offset));
    }

    public record AttrWithOffset(@NotNull Method method, @NotNull RTTI.Attr attr, int offset) {}
}
