package com.shade.decima.rtti;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class RTTI {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Attr {
        String name();

        String type();

        int position();

        int offset();

        int flags() default 0;

        String min() default "";

        String max() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Category {
        String name();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    public @interface Base {
        int offset();
    }

    public sealed interface Value {
        non-sealed interface OfByte extends Value {
            byte value();
        }

        non-sealed interface OfShort extends Value {
            short value();
        }

        non-sealed interface OfInt extends Value {
            @NotNull
            static <T extends OfInt> Optional<T> valueOf(@NotNull Class<T> enumClass, int value) {
                for (T constant : enumClass.getEnumConstants()) {
                    if (constant.value() == value) {
                        return Optional.of(constant);
                    }
                }
                return Optional.empty();
            }

            int value();
        }
    }

    private RTTI() {
    }

    @NotNull
    public static Class<?> getType(@NotNull String name, @NotNull Class<?> namespace) {
        for (Class<?> cls : namespace.getDeclaredClasses()) {
            if (cls.getSimpleName().equals(name)) {
                return cls;
            }
        }
        throw new IllegalStateException("Type not found: " + name);
    }

    @NotNull
    public static List<OrderedAttr> getAttributes(@NotNull Class<?> cls) {
        List<OrderedAttr> attrs = new ArrayList<>();
        collectAttrs(cls, cls, attrs, 0);
        attrs.removeIf(attr -> (attr.flags & 2) != 0); // remove save-state only
        sortAttrs(attrs);
        return attrs;
    }

    private static void collectAttrs(
        @NotNull Class<?> parent,
        @NotNull Class<?> cls,
        @NotNull List<OrderedAttr> attrs,
        int offset
    ) {
        for (AnnotatedType type : cls.getAnnotatedInterfaces()) {
            Base base = type.getDeclaredAnnotation(Base.class);
            if (base == null) {
                throw new IllegalStateException("Unexpected interface: " + type);
            }
            collectAttrs(parent, (Class<?>) type.getType(), attrs, base.offset() + offset);
        }
        List<OrderedAttr> classAttrs = new ArrayList<>();
        int position = 0;
        for (Method method : cls.getDeclaredMethods()) {
            if (!Modifier.isAbstract(method.getModifiers())) {
                // Skips overridden methods (e.g. categories)
                continue;
            }
            Category category = method.getDeclaredAnnotation(Category.class);
            if (category == null) {
                collectAttr(parent, method, null, classAttrs, offset, position);
                position++;
            } else {
                position = collectCategoryAttrs(parent, method.getReturnType(), category.name(), classAttrs, position, offset);
            }
        }
        classAttrs.sort(Comparator.comparingInt(attr -> attr.position));
        attrs.addAll(classAttrs);
    }

    private static int collectCategoryAttrs(
        @NotNull Class<?> parent,
        @NotNull Class<?> cls,
        @NotNull String category,
        @NotNull List<OrderedAttr> attrs,
        int position,
        int offset
    ) {
        for (Method method : cls.getDeclaredMethods()) {
            collectAttr(parent, method, category, attrs, position, offset);
            position++;
        }
        return position;
    }

    private static void collectAttr(
        @NotNull Class<?> parent,
        @NotNull Method method,
        @Nullable String category,
        @NotNull List<OrderedAttr> attrs,
        int position,
        int offset
    ) {
        Attr attr = method.getDeclaredAnnotation(Attr.class);
        if (attr != null) {
            attrs.add(new OrderedAttr(attr.name(), category, parent, attr.type(), method.getGenericReturnType(), position, attr.offset() + offset, attr.flags()));
        } else if (method.getReturnType() != void.class) {
            throw new IllegalStateException("Unexpected method: " + method);
        }
    }

    private static void sortAttrs(@NotNull List<OrderedAttr> attrs) {
        quicksort(attrs, Comparator.comparingInt(attr -> attr.offset));
    }

    private static <T> void quicksort(@NotNull List<T> items, @NotNull Comparator<T> comparator) {
        quicksort(items, comparator, 0, items.size() - 1, 0);
    }

    private static <T> int quicksort(@NotNull List<T> items, @NotNull Comparator<T> comparator, int left, int right, int state) {
        if (left < right) {
            state = 0x19660D * state + 0x3C6EF35F;

            int pivot = (state >>> 8) % (right - left);
            swap(items, left + pivot, right);

            int start = partition(items, comparator, left, right);
            state = quicksort(items, comparator, left, start - 1, state);
            state = quicksort(items, comparator, start + 1, right, state);
        }

        return state;
    }

    private static <T> int partition(@NotNull List<T> items, @NotNull Comparator<T> comparator, int left, int right) {
        int start = left - 1;
        int end = right;

        while (true) {
            do {
                start++;
            } while (start < end && comparator.compare(items.get(start), items.get(right)) < 0);

            do {
                end--;
            } while (end > start && comparator.compare(items.get(right), items.get(end)) < 0);

            if (start >= end) {
                break;
            }

            swap(items, start, end);
        }

        swap(items, start, right);

        return start;
    }

    private static <T> void swap(@NotNull List<T> items, int a, int b) {
        T item = items.get(a);
        items.set(a, items.get(b));
        items.set(b, item);
    }

    public static final class OrderedAttr {
        private final String name;
        private final String categoryName;
        private final String typeName;
        private final Type type;
        private final Class<?> parent;
        private final int position;
        private final int offset;
        private final int flags;

        public OrderedAttr(
            @NotNull String name,
            @Nullable String categoryName,
            @NotNull Class<?> parent,
            @NotNull String typeName,
            @NotNull Type type,
            int position,
            int offset,
            int flags
        ) {
            this.name = name;
            this.categoryName = categoryName;
            this.parent = parent;
            this.typeName = typeName;
            this.type = type;
            this.position = position;
            this.offset = offset;
            this.flags = flags;
        }

        @NotNull
        public String name() {
            return name;
        }

        @NotNull
        public String typeName() {
            return typeName;
        }

        @Nullable
        public String categoryName() {
            return categoryName;
        }

        @NotNull
        public Class<?> parent() {
            return parent;
        }

        @NotNull
        public Type type() {
            return type;
        }
    }
}
