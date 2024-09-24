package com.shade.decima.rtti;

import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.objectweb.asm.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.objectweb.asm.Opcodes.*;

public class RTTI {
    public static final String CALLBACK_FIELD_NAME = "EXTRA_BINARY_DATA_CALLBACK";

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Serializable {
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

    public interface ValueEnum<T extends Number> {
        @Nullable
        static <T extends Enum<T> & ValueEnum<V>, V extends Number> T valueOf(
            @NotNull Class<T> enumClass,
            @NotNull V value
        ) {
            for (T constant : enumClass.getEnumConstants()) {
                if (constant.value().equals(value)) {
                    return constant;
                }
            }
            return null;
        }

        T value();
    }

    public interface ValueSetEnum<T extends Number> extends ValueEnum<T> {
        @NotNull
        static <T extends Enum<T> & ValueSetEnum<V>, V extends Number> Set<T> setOf(
            @NotNull Class<T> enumClass,
            @NotNull V value
        ) {
            Set<T> set = EnumSet.noneOf(enumClass);
            for (T constant : enumClass.getEnumConstants()) {
                if (constant.contains(value)) {
                    set.add(constant);
                }
            }
            return set;
        }

        default boolean contains(@NotNull T value) {
            return (value().longValue() & value.longValue()) != 0;
        }
    }

    private static final Handle BOOTSTRAP_HANDLE = new Handle(
        H_INVOKESTATIC,
        "java/lang/runtime/ObjectMethods",
        "bootstrap",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;",
        false
    );

    private static final Map<Class<?>, Map<String, Class<?>>> namespaceCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Class<?>> representationCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<AttributeInfo>> attributeCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<CategoryInfo>> categoryCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, ExtraBinaryDataCallback<?>> callbackCache = new ConcurrentHashMap<>();

    private RTTI() {
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
    public static Class<?> getRepresentationType(@NotNull Class<?> cls) {
        return representationCache.computeIfAbsent(cls, RTTI::getRepresentationType0);
    }

    @NotNull
    public static List<AttributeInfo> getAttributes(@NotNull Class<?> cls) {
        return attributeCache.computeIfAbsent(cls, RTTI::getAttributes0);
    }

    @NotNull
    public static List<CategoryInfo> getCategories(@NotNull Class<?> cls) {
        return categoryCache.computeIfAbsent(cls, RTTI::getCategories0);
    }

    @Nullable
    public static ExtraBinaryDataCallback<?> getExtraBinaryDataCallback(@NotNull Class<?> cls) {
        return callbackCache.computeIfAbsent(cls, RTTI::getExtraBinaryDataCallback0);
    }

    @NotNull
    public static <T> T newInstance(@NotNull Class<T> cls) {
        if (representationCache.containsValue(cls)) {
            throw new IllegalStateException("Can't create an instance of representation type " + cls
                + ". Use " + cls.getInterfaces()[0] + " instead");
        }
        Class<?> type = getRepresentationType(cls);
        try {
            return cls.cast(type.getConstructor().newInstance());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create an instance of " + cls, e);
        }
    }

    @Nullable
    private static ExtraBinaryDataCallback<?> getExtraBinaryDataCallback0(@NotNull Class<?> cls) {
        Field field;
        try {
            field = cls.getField(CALLBACK_FIELD_NAME);
        } catch (NoSuchFieldException e) {
            return null;
        }
        try {
            return (ExtraBinaryDataCallback<?>) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to get extra binary data callback", e);
        }
    }

    @NotNull
    private static List<CategoryInfo> getCategories0(@NotNull Class<?> cls) {
        return getAttributes(cls).stream()
            .map(AttributeInfo::category)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(CategoryInfo::name))
            .distinct()
            .toList();
    }

    @NotNull
    private static Map<String, Class<?>> getNamespaceTypes(@NotNull Class<?> namespace) {
        Map<String, Class<?>> map = new HashMap<>();
        for (Class<?> cls : namespace.getDeclaredClasses()) {
            map.put(cls.getSimpleName(), cls);
        }
        return map;
    }

    @NotNull
    private static Class<?> getRepresentationType0(@NotNull Class<?> cls) {
        String className = Type.getInternalName(cls) + "$POD";

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, null, Type.getInternalName(Object.class), new String[]{Type.getInternalName(cls)});
        cw.visitEnd();

        MethodVisitor init = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(ALOAD, 0);
        init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

        for (CategoryInfo category : getCategories(cls)) {
            { // field
                FieldVisitor fv = cw.visitField(ACC_PRIVATE | ACC_FINAL, category.name, Type.getDescriptor(category.type), null, null);
                fv.visitEnd();
            }

            { // getter
                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, category.getter.getName(), Type.getMethodDescriptor(category.getter), null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, category.name, Type.getDescriptor(category.type));
                mv.visitInsn(ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            { // initialize in constructor
                init.visitVarInsn(ALOAD, 0);
                init.visitLdcInsn(Type.getType(category.type));
                init.visitMethodInsn(INVOKESTATIC, Type.getInternalName(RTTI.class), "newInstance", "(Ljava/lang/Class;)Ljava/lang/Object;", false);
                init.visitFieldInsn(PUTFIELD, className, category.name, Type.getDescriptor(category.type));
            }
        }

        init.visitInsn(RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        for (AttributeInfo attr : getAttributes(cls)) {
            if (attr.category != null) {
                continue;
            }

            Type type = Type.getReturnType(attr.getter);
            String name = attr.name;

            { // field
                FieldVisitor fv = cw.visitField(ACC_PRIVATE, name, type.getDescriptor(), null, null);
                fv.visitEnd();
            }

            { // getter
                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, attr.getter.getName(), Type.getMethodDescriptor(attr.getter), null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, name, type.getDescriptor());
                mv.visitInsn(type.getOpcode(IRETURN));
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            { // setter
                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, attr.setter.getName(), Type.getMethodDescriptor(attr.setter), null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(type.getOpcode(ILOAD), 1);
                mv.visitFieldInsn(PUTFIELD, className, name, type.getDescriptor());
                mv.visitInsn(RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        }

        { // toString, equals, hashCode
            List<String> names = new ArrayList<>();
            List<Handle> handles = new ArrayList<>();

            for (AttributeInfo attr : getAttributes(cls)) {
                if (attr.category != null) {
                    continue;
                }
                names.add(attr.name);
                handles.add(new Handle(
                    H_INVOKEINTERFACE,
                    Type.getInternalName(cls),
                    attr.getter.getName(),
                    Type.getMethodDescriptor(Type.getReturnType(attr.getter)),
                    true
                ));
            }

            List<Object> args = new ArrayList<>(2 + handles.size());
            args.add(Type.getType(cls));
            args.add(String.join(";", names));
            args.addAll(handles);

            MethodVisitor mv;

            mv = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, "equals", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)), null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInvokeDynamicInsn("equals", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(cls), Type.getType(Object.class)), BOOTSTRAP_HANDLE, args.toArray());
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, "hashCode", Type.getMethodDescriptor(Type.INT_TYPE), null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInvokeDynamicInsn("hashCode", Type.getMethodDescriptor(Type.INT_TYPE, Type.getType(cls)), BOOTSTRAP_HANDLE, args.toArray());
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, "toString", Type.getMethodDescriptor(Type.getType(String.class)), null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInvokeDynamicInsn("toString", Type.getMethodDescriptor(Type.getType(String.class), Type.getType(cls)), BOOTSTRAP_HANDLE, args.toArray());
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        try {
            return MethodHandles.privateLookupIn(cls, MethodHandles.lookup())
                .defineHiddenClass(cw.toByteArray(), false)
                .lookupClass();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to create representation type", e);
        }
    }

    @NotNull
    private static List<AttributeInfo> getAttributes0(@NotNull Class<?> cls) {
        List<AttributeInfo> attrs = new ArrayList<>();
        collectAttrs(cls, attrs, 0);
        filterAttrs(attrs);
        sortAttrs(attrs);
        return attrs;
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
                collectCategoryAttrs(method.getReturnType(), category.name(), method, sorted, offset, serializable);
            } else {
                collectAttr(method, null, sorted, offset, serializable);
            }
        }
        sorted.sort(Comparator.comparingInt(info -> info.position));
        attrs.addAll(sorted);
    }

    private static void collectCategoryAttrs(
        @NotNull Class<?> cls,
        @NotNull String name,
        @NotNull Method getter,
        @NotNull List<AttributeInfo> attrs,
        int offset,
        boolean serializable
    ) {
        CategoryInfo category = new CategoryInfo(name, cls, getter);
        for (Method method : cls.getDeclaredMethods()) {
            collectAttr(method, category, attrs, offset, serializable);
        }
    }

    private static void collectAttr(
        @NotNull Method method,
        @Nullable CategoryInfo category,
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
                method,
                setter,
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
        quicksort(items, comparator, 0, items.size() - 1, 0);
    }

    private static <T> int quicksort(
        @NotNull List<T> items,
        @NotNull Comparator<T> comparator,
        int left,
        int right,
        int state
    ) {
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
        private final Method getter;
        private final Method setter;

        private final int position;
        private final int offset;
        private final int flags;
        private final boolean serializable;

        private AttributeInfo(
            @NotNull String name,
            @Nullable CategoryInfo category,
            @NotNull String typeName,
            @NotNull java.lang.reflect.Type type,
            @NotNull Method getter,
            @NotNull Method setter,
            int position,
            int offset,
            int flags,
            boolean serializable
        ) {
            this.name = name;
            this.category = category;
            this.typeName = TypeName.of(typeName);
            this.type = type;
            this.getter = getter;
            this.setter = setter;
            this.position = position;
            this.offset = offset;
            this.flags = flags;
            this.serializable = serializable;
        }

        @NotNull
        public String name() {
            return name;
        }

        @Nullable
        public CategoryInfo category() {
            return category;
        }

        @NotNull
        public TypeName typeName() {
            return typeName;
        }

        @NotNull
        public java.lang.reflect.Type type() {
            return type;
        }

        public boolean serializable() {
            return serializable;
        }

        @Nullable
        public Object get(@NotNull Object instance) {
            try {
                if (category != null) {
                    instance = category.getter.invoke(instance);
                }
                return getter.invoke(instance);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to get attribute: " + name, e);
            }
        }

        public void set(@NotNull Object instance, @Nullable Object value) {
            try {
                if (category != null) {
                    instance = category.getter.invoke(instance);
                }
                setter.invoke(instance, value);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to set attribute: " + name, e);
            }
        }

        @Override
        public String toString() {
            return typeName + " " + name;
        }
    }

    private record BaseInfo(@NotNull Class<?> cls, int offset) {}
}
