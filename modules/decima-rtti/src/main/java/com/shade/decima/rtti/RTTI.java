package com.shade.decima.rtti;

import com.shade.decima.rtti.data.Ref;
import com.shade.decima.rtti.data.Value;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.objectweb.asm.Handle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;

public class RTTI {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Serializable {
        int version() default 0;

        int flags() default 0;

        int size() default 0;
    }

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

    public interface Compound {
        Class<?> getType();
    }

    private static final Handle BOOTSTRAP_HANDLE = new Handle(
        H_INVOKESTATIC,
        "java/lang/runtime/ObjectMethods",
        "bootstrap",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;",
        false
    );

    private static final Map<Class<?>, Map<String, Class<?>>> namespaceCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, RepresentationInfo> representationCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<AttributeInfo>> attributeCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<CategoryInfo>> categoryCache = new ConcurrentHashMap<>();

    private RTTI() {
    }

    @NotNull
    public static String getTypeName(@NotNull Object object) {
        return getType(object).getSimpleName();
    }

    @NotNull
    public static Class<?> getType(@NotNull Object object) {
        if (object instanceof Compound compound) {
            return compound.getType();
        } else {
            return object.getClass();
        }
    }

    @NotNull
    public static Class<?> getType(@NotNull Class<?> cls) {
        if (!isCompound(cls)) {
            throw new IllegalStateException("Can't get type of a non-compound type");
        }
        if (Compound.class.isAssignableFrom(cls)) {
            // Runtime representation
            return cls.getInterfaces()[0];
        } else {
            return cls;
        }
    }

    public static boolean isCompound(@NotNull Class<?> cls) {
        return Compound.class.isAssignableFrom(cls)
            || cls.isInterface() && !isContainer(cls) && !isPointer(cls);
    }

    public static boolean isEnum(@NotNull Class<?> cls) {
        return cls.isEnum() && Value.class.isAssignableFrom(cls);
    }

    public static boolean isContainer(@NotNull Class<?> cls) {
        return cls.isArray() || List.class.isAssignableFrom(cls);
    }

    public static boolean isPointer(@NotNull Class<?> cls) {
        return Ref.class.isAssignableFrom(cls);
    }

    public static boolean isAtom(@NotNull Class<?> cls) {
        return cls.isPrimitive() || String.class.isAssignableFrom(cls) || Number.class.isAssignableFrom(cls);
    }

    @NotNull
    public static Class<?> getType(@NotNull String name, @NotNull Class<?> namespace) {
        Class<?> cls = namespaceCache
            .computeIfAbsent(namespace, RTTI::getNamespaceTypes)
            .get(name);
        if (cls == null) {
            throw new IllegalStateException("Type not found: " + name);
        }
        return cls;
    }

    @NotNull
    public static Collection<Class<?>> getTypes(@NotNull Class<?> namespace) {
        return Collections.unmodifiableCollection(namespaceCache.computeIfAbsent(namespace, RTTI::getNamespaceTypes).values());
    }

    @NotNull
    public static List<AttributeInfo> getAttrsSorted(@NotNull Class<?> cls) {
        if (!isCompound(cls)) {
            throw new IllegalStateException("Can't get attributes of a non-compound type");
        }
        return attributeCache.computeIfAbsent(getType(cls), RTTI::getAttrsSorted0);
    }

    @NotNull
    public static List<CategoryInfo> getCategories(@NotNull Class<?> cls) {
        if (!isCompound(cls)) {
            throw new IllegalStateException("Can't get categories of a non-compound type");
        }
        return categoryCache.computeIfAbsent(getType(cls), RTTI::getCategories0);
    }

    @NotNull
    public static <T> T newInstance(@NotNull Class<T> cls) {
        RepresentationInfo type = getRepresentationInfo(cls);
        try {
            return cls.cast(type.constructor.invoke());
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to create an instance of " + cls, e);
        }
    }

    @NotNull
    private static RepresentationInfo getRepresentationInfo(@NotNull Class<?> cls) {
        if (!isCompound(cls)) {
            throw new IllegalStateException("Can't get representation of a non-compound type");
        }
        return representationCache.computeIfAbsent(getType(cls), RTTI::getRepresentationInfo0);
    }

    @NotNull
    private static List<CategoryInfo> getCategories0(@NotNull Class<?> cls) {
        List<CategoryInfo> categories = new ArrayList<>();
        for (Method method : cls.getMethods()) {
            if (!Modifier.isAbstract(method.getModifiers())) {
                // Skips overridden methods (e.g. categories)
                continue;
            }
            Category category = method.getDeclaredAnnotation(Category.class);
            if (category != null) {
                categories.add(new CategoryInfo(category.name(), method.getReturnType(), method));
            }
        }
        categories.sort(Comparator.comparing(CategoryInfo::name));
        return categories;
    }

    @NotNull
    private static Map<String, Class<?>> getNamespaceTypes(@NotNull Class<?> namespace) {
        Map<String, Class<?>> map = new HashMap<>();
        for (Class<?> cls : namespace.getDeclaredClasses()) {
            if (!cls.isInterface()) {
                continue;
            }
            map.put(cls.getSimpleName(), cls);
        }
        return map;
    }

    @NotNull
    private static RepresentationInfo getRepresentationInfo0(@NotNull Class<?> iface) {
        try {
            var lookup = MethodHandles.privateLookupIn(iface, MethodHandles.lookup());
            var clazz = RuntimeTypeGenerator.generate(iface, lookup);
            var constructor = lookup.findConstructor(clazz, MethodType.methodType(void.class));

            return new RepresentationInfo(clazz, constructor);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create representation type", e);
        }
    }

    @NotNull
    private static List<AttributeInfo> getAttrsSorted0(@NotNull Class<?> cls) {
        List<AttributeInfo> attrs = new ArrayList<>();
        collectAttrs(cls, attrs, 0);
        filterAttrs(attrs);
        sortAttrs(attrs);
        return Collections.unmodifiableList(attrs);
    }

    private static void filterAttrs(@NotNull List<AttributeInfo> attrs) {
        attrs.removeIf(attr -> (attr.flags & 2) != 0); // remove save-state only
    }

    private static void collectAttrs(
        @NotNull Class<?> cls,
        @NotNull List<AttributeInfo> attrs,
        int offset
    ) {
        for (BaseInfo base : collectBases(cls)) {
            collectAttrs(base.cls, attrs, base.offset + offset);
        }
        collectAttrs(cls, attrs, offset, cls.isAnnotationPresent(Serializable.class));
    }

    private static void collectAttrs(
        @NotNull Class<?> cls,
        @NotNull List<AttributeInfo> attrs,
        int offset,
        boolean serializable
    ) {
        List<AttributeInfo> sorted = new ArrayList<>();
        for (Method method : cls.getDeclaredMethods()) {
            if (!Modifier.isAbstract(method.getModifiers())) {
                // Skips overridden methods (e.g. categories)
                continue;
            }
            Category category = method.getDeclaredAnnotation(Category.class);
            if (category != null) {
                collectCategoryAttrs(method.getReturnType(), category.name(), method, cls, sorted, offset, serializable);
            } else {
                collectAttr(method, null, cls, sorted, offset, serializable);
            }
        }
        sorted.sort(Comparator.comparingInt(info -> info.position));
        attrs.addAll(sorted);
    }

    private static void collectCategoryAttrs(
        @NotNull Class<?> cls,
        @NotNull String name,
        @NotNull Method getter,
        @NotNull Class<?> parent,
        @NotNull List<AttributeInfo> attrs,
        int offset,
        boolean serializable
    ) {
        CategoryInfo category = new CategoryInfo(name, cls, getter);
        for (Method method : cls.getDeclaredMethods()) {
            collectAttr(method, category, parent, attrs, offset, serializable);
        }
    }

    private static void collectAttr(
        @NotNull Method method,
        @Nullable CategoryInfo category,
        @NotNull Class<?> parent,
        @NotNull List<AttributeInfo> attrs,
        int offset,
        boolean serializable
    ) {
        Attr attr = method.getDeclaredAnnotation(Attr.class);
        if (attr != null) {
            Method setter;
            try {
                setter = method.getDeclaringClass().getDeclaredMethod(method.getName(), method.getReturnType());
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Setter not found: " + method, e);
            }
            attrs.add(new AttributeInfo(
                attr.name(),
                category,
                attr.type(),
                method.getGenericReturnType(),
                parent,
                attr.position(),
                attr.offset() + offset,
                attr.flags(),
                serializable
            ));
        } else if (method.getReturnType() != void.class) {
            throw new IllegalStateException("Unexpected method: " + method);
        }
    }

    @NotNull
    private static List<BaseInfo> collectBases(@NotNull Class<?> cls) {
        List<BaseInfo> bases = new ArrayList<>();
        for (AnnotatedType type : cls.getAnnotatedInterfaces()) {
            var base = type.getDeclaredAnnotation(Base.class);
            var offset = base != null ? base.offset() : 0;
            bases.add(new BaseInfo((Class<?>) type.getType(), offset));
        }
        bases.sort(Comparator.comparingInt(BaseInfo::offset));
        return bases;
    }

    private static void sortAttrs(@NotNull List<AttributeInfo> attrs) {
        quicksort(attrs, Comparator.comparingInt(attr -> attr.offset));
    }

    private static <T> void quicksort(@NotNull List<T> items, @NotNull Comparator<T> comparator) {
        quicksort(items, 0, items.size() - 1, comparator, 0);
    }

    private static <T> int quicksort(
        @NotNull List<T> items,
        int left,
        int right,
        @NotNull Comparator<T> comparator,
        int state
    ) {
        if (left < right) {
            state = 0x19660D * state + 0x3C6EF35F;

            int pivot = (state >>> 8) % (right - left);
            Collections.swap(items, left + pivot, left);

            int start = partition(items, left, right, comparator);
            state = quicksort(items, left, start - 1, comparator, state);
            state = quicksort(items, start + 1, right, comparator, state);
        }

        return state;
    }

    private static <T> int partition(@NotNull List<T> items, int left, int right, @NotNull Comparator<T> comparator) {
        var l = left - 1;
        var r = right;

        while (true) {
            do {
                if (l >= r) {
                    break;
                }
                l++;
            } while (comparator.compare(items.get(l), items.get(right)) < 0);

            do {
                if (r <= l) {
                    break;
                }
                r--;
            } while (comparator.compare(items.get(right), items.get(r)) < 0);

            if (l >= r) {
                break;
            }

            Collections.swap(items, l, r);
        }

        Collections.swap(items, l, right);

        return l;
    }

    public record CategoryInfo(
        @NotNull String name,
        @NotNull Class<?> type,
        @NotNull Method getter
    ) {
    }

    public static final class AttributeInfo {
        private final String name;
        private final CategoryInfo category;
        private final TypeName typeName;
        private final java.lang.reflect.Type type;
        private final Class<?> parent;

        private final int position;
        private final int offset;
        private final int flags;
        private final boolean serializable;

        private VarHandle handle;

        private AttributeInfo(
            @NotNull String name,
            @Nullable CategoryInfo category,
            @NotNull String typeName,
            @NotNull java.lang.reflect.Type type,
            @NotNull Class<?> parent,
            int position,
            int offset,
            int flags,
            boolean serializable
        ) {
            this.name = name;
            this.category = category;
            this.typeName = TypeName.parse(typeName);
            this.type = type;
            this.parent = parent;
            this.position = position;
            this.offset = offset;
            this.flags = flags;
            this.serializable = serializable;
        }

        @NotNull
        public String name() {
            return name;
        }

        @NotNull
        public TypeName typeName() {
            return typeName;
        }

        @NotNull
        public java.lang.reflect.Type type() {
            return type;
        }

        public int flags() {
            return flags;
        }

        public int offset() {
            return offset;
        }

        public int position() {
            return position;
        }

        public boolean serializable() {
            return serializable;
        }

        public Object get(@NotNull Object instance) {
            try {
                return fieldHandle(instance).get(instance);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to get attribute: " + name, e);
            }
        }

        public void set(@NotNull Object instance, @Nullable Object value) {
            try {
                fieldHandle(instance).set(instance, value);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to set attribute: " + name, e);
            }
        }

        @Override
        public String toString() {
            return typeName + " " + parent.getSimpleName() + "." + name;
        }

        @NotNull
        private String fieldName() {
            if (category != null) {
                return category.name + '$' + name;
            } else {
                return name;
            }
        }

        @NotNull
        private VarHandle fieldHandle(@NotNull Object object) {
            if (handle == null) {
                if (!parent.isInstance(object)) {
                    throw new IllegalArgumentException("Object is not an instance of " + parent);
                }
                try {
                    var cls = object.getClass();
                    var lookup = MethodHandles.privateLookupIn(cls, MethodHandles.lookup());
                    var rawType = (Class<?>) (type instanceof ParameterizedType p ? p.getRawType() : type);
                    handle = lookup.findVarHandle(cls, fieldName(), rawType);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Failed to get field handle", e);
                }
            }
            return handle;
        }
    }

    private record BaseInfo(@NotNull Class<?> cls, int offset) {}

    private record RepresentationInfo(
        @NotNull Class<?> cls,
        @NotNull MethodHandle constructor
    ) {}
}
