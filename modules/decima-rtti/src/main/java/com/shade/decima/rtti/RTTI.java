package com.shade.decima.rtti;

import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.objectweb.asm.*;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.objectweb.asm.Opcodes.*;

public class RTTI {
    private static final Handle BOOTSTRAP_HANDLE = new Handle(
        H_INVOKESTATIC,
        "java/lang/runtime/ObjectMethods",
        "bootstrap",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;",
        false
    );

    private static final Map<Class<?>, Map<String, Class<?>>> namespaceCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Class<?>> representationCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<OrderedAttr>> attributeCache = new ConcurrentHashMap<>();

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
    public static List<OrderedAttr> getAttributes(@NotNull Class<?> cls) {
        return attributeCache.computeIfAbsent(cls, RTTI::getAttributes0);
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

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        for (OrderedAttr attr : getAttributes(cls)) {
            Type type = Type.getReturnType(attr.getter);
            String name = attr.name;

            if (attr.category == null) {
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
        }

        { // toString, equals, hashCode
            List<String> names = new ArrayList<>();
            List<Handle> handles = new ArrayList<>();

            for (OrderedAttr attr : getAttributes(cls)) {
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
            mv = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, "toString", Type.getMethodDescriptor(Type.getType(String.class)), null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInvokeDynamicInsn("toString", Type.getMethodDescriptor(Type.getType(String.class), Type.getType(cls)), BOOTSTRAP_HANDLE, args.toArray());
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, "equals", Type.getMethodDescriptor(Type.getType(boolean.class), Type.getType(Object.class)), null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInvokeDynamicInsn("equals", Type.getMethodDescriptor(Type.getType(boolean.class), Type.getType(cls), Type.getType(Object.class)), BOOTSTRAP_HANDLE, args.toArray());
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, "hashCode", Type.getMethodDescriptor(Type.getType(int.class)), null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInvokeDynamicInsn("hashCode", Type.getMethodDescriptor(Type.getType(int.class), Type.getType(cls)), BOOTSTRAP_HANDLE, args.toArray());
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        try {
            Files.write(Path.of(cls.getSimpleName() + "$POD.class"), cw.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write representation type", e);
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
    private static List<OrderedAttr> getAttributes0(@NotNull Class<?> cls) {
        List<OrderedAttr> attrs = new ArrayList<>();
        collectAttrs(cls, cls, attrs, 0);
        filterAttrs(attrs);
        sortAttrs(attrs);
        return attrs;
    }

    private static void filterAttrs(@NotNull List<OrderedAttr> attrs) {
        attrs.removeIf(attr -> (attr.flags & 2) != 0); // remove save-state only
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
            Method setter;
            try {
                setter = method.getDeclaringClass().getDeclaredMethod(method.getName(), method.getReturnType());
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Setter not found: " + method, e);
            }
            attrs.add(new OrderedAttr(
                attr.name(),
                category,
                parent,
                attr.type(),
                method.getGenericReturnType(),
                method,
                setter,
                position,
                attr.offset() + offset,
                attr.flags()
            ));
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

    public static final class OrderedAttr {
        private final String name;
        private final String category;
        private final Class<?> parent;

        private final String typeName;
        private final java.lang.reflect.Type type;
        private final Method getter;
        private final Method setter;

        private final int position;
        private final int offset;
        private final int flags;

        private OrderedAttr(
            @NotNull String name,
            @Nullable String category,
            @NotNull Class<?> parent,
            @NotNull String typeName,
            @NotNull java.lang.reflect.Type type,
            @NotNull Method getter,
            @NotNull Method setter,
            int position,
            int offset,
            int flags
        ) {
            this.name = name;
            this.category = category;
            this.parent = parent;
            this.typeName = typeName;
            this.type = type;
            this.getter = getter;
            this.setter = setter;
            this.position = position;
            this.offset = offset;
            this.flags = flags;
        }

        @NotNull
        public String name() {
            return name;
        }

        @Nullable
        public String category() {
            return category;
        }

        @NotNull
        public Class<?> parent() {
            return parent;
        }

        @NotNull
        public String typeName() {
            return typeName;
        }

        @NotNull
        public java.lang.reflect.Type type() {
            return type;
        }
    }
}
