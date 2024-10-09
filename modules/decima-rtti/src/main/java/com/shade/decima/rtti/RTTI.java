package com.shade.decima.rtti;

import com.shade.decima.rtti.generator.TypeGenerator;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.objectweb.asm.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.objectweb.asm.Opcodes.*;

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

    public interface ValueEnum {
        @Nullable
        static <T extends Enum<T> & ValueEnum> T valueOf(@NotNull Class<T> enumClass, int value) {
            for (T constant : enumClass.getEnumConstants()) {
                if (constant.value() == value) {
                    return constant;
                }
            }
            return null;
        }

        int value();
    }

    public interface ValueSetEnum extends ValueEnum {
        @NotNull
        static <T extends Enum<T> & ValueSetEnum> Set<T> setOf(@NotNull Class<T> enumClass, int value) {
            Set<T> set = EnumSet.noneOf(enumClass);
            for (T constant : enumClass.getEnumConstants()) {
                if ((value & constant.value()) != 0) {
                    set.add(constant);
                    value &= ~constant.value();
                }
            }
            return set;
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
    private static final Map<Class<?>, RepresentationInfo> representationCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<AttributeInfo>> attributeCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<CategoryInfo>> categoryCache = new ConcurrentHashMap<>();

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
    public static Collection<Class<?>> getTypes(@NotNull Class<?> namespace) {
        return Collections.unmodifiableCollection(namespaceCache.computeIfAbsent(namespace, RTTI::getNamespaceTypes).values());
    }

    @NotNull
    public static List<AttributeInfo> getAttrsSorted(@NotNull Class<?> cls) {
        return attributeCache.computeIfAbsent(cls, RTTI::getAttrsSorted0);
    }

    @NotNull
    public static List<CategoryInfo> getCategories(@NotNull Class<?> cls) {
        return categoryCache.computeIfAbsent(cls, RTTI::getCategories0);
    }

    @Nullable
    public static ExtraBinaryDataCallback<?> getExtraBinaryDataCallback(@NotNull Class<?> cls) {
        return getRepresentationInfo(cls).extraBinaryDataCallback;
    }

    @NotNull
    public static <T> T newInstance(@NotNull Class<T> cls) {
        if (!cls.isInterface()) {
            throw new IllegalStateException("Can't create an instance of representation type " + cls
                + ". Use " + cls.getInterfaces()[0] + " instead");
        }
        RepresentationInfo type = getRepresentationInfo(cls);
        try {
            return cls.cast(type.constructor.invoke());
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to create an instance of " + cls, e);
        }
    }

    @NotNull
    private static RepresentationInfo getRepresentationInfo(@NotNull Class<?> cls) {
        return representationCache.computeIfAbsent(cls, RTTI::getRepresentationInfo0);
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
            map.put(cls.getSimpleName(), cls);
        }
        return map;
    }

    @NotNull
    private static RepresentationInfo getRepresentationInfo0(@NotNull Class<?> iface) {
        try {
            var lookup = MethodHandles.privateLookupIn(iface, MethodHandles.lookup());
            var clazz = generateClass(iface, lookup);
            var constructor = lookup.findConstructor(clazz, MethodType.methodType(void.class));
            var extraBinaryDataCallback = getExtraBinaryDataCallback0(iface);

            return new RepresentationInfo(clazz, constructor, extraBinaryDataCallback);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create representation type", e);
        }
    }

    @NotNull
    private static Class<?> generateClass(
        @NotNull Class<?> iface,
        @NotNull MethodHandles.Lookup lookup
    ) throws IllegalAccessException {
        var type = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        var typeName = Type.getType('L' + Type.getInternalName(iface) + "$POD;");
        type.visit(V11, ACC_PUBLIC | ACC_SUPER, typeName.getInternalName(), null, "java/lang/Object", new String[]{Type.getInternalName(iface)});

        var init = type.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(ALOAD, 0);
        init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

        var categoryIndex = 1;
        for (CategoryInfo category : getCategories(iface)) {
            var categoryType = Type.getReturnType(category.getter);
            var categoryName = Type.getType('L' + typeName.getInternalName() + '$' + (categoryIndex++) + ';');

            var getter = type.visitMethod(ACC_PUBLIC, category.getter.getName(), Type.getMethodDescriptor(category.getter), null, null);
            getter.visitCode();
            getter.visitVarInsn(ALOAD, 0);
            getter.visitFieldInsn(GETFIELD, typeName.getInternalName(), category.name, categoryType.getDescriptor());
            getter.visitInsn(ARETURN);
            getter.visitMaxs(0, 0);
            getter.visitEnd();

            init.visitVarInsn(ALOAD, 0);
            init.visitTypeInsn(NEW, categoryName.getInternalName());
            init.visitInsn(DUP);
            init.visitVarInsn(ALOAD, 0);
            init.visitMethodInsn(INVOKESPECIAL, categoryName.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, typeName), false);
            init.visitFieldInsn(PUTFIELD, typeName.getInternalName(), category.name, categoryType.getDescriptor());

            type.visitField(ACC_PRIVATE | ACC_FINAL, category.name, categoryType.getDescriptor(), null, null).visitEnd();
            type.visitNestMember(categoryName.getInternalName());
            type.visitInnerClass(categoryName.getInternalName(), null, null, 0);

            generateCategory(typeName, categoryName, category, lookup);
        }

        for (AttributeInfo attr : getAttrsSorted(iface)) {
            var attrType = Type.getReturnType(attr.getter);

            type.visitField(ACC_PRIVATE, attr.fieldName(), attrType.getDescriptor(), null, null).visitEnd();

            if (attr.category != null) {
                continue;
            }

            var getter = type.visitMethod(ACC_PUBLIC, attr.getter.getName(), Type.getMethodDescriptor(attr.getter), null, null);
            getter.visitCode();
            getter.visitVarInsn(ALOAD, 0);
            getter.visitFieldInsn(GETFIELD, typeName.getInternalName(), attr.name(), attrType.getDescriptor());
            getter.visitInsn(attrType.getOpcode(IRETURN));
            getter.visitMaxs(0, 0);
            getter.visitEnd();

            var setter = type.visitMethod(ACC_PUBLIC, attr.setter.getName(), Type.getMethodDescriptor(attr.setter), null, null);
            setter.visitCode();
            setter.visitVarInsn(ALOAD, 0);
            setter.visitVarInsn(attrType.getOpcode(ILOAD), 1);
            setter.visitFieldInsn(PUTFIELD, typeName.getInternalName(), attr.name(), attrType.getDescriptor());
            setter.visitInsn(RETURN);
            setter.visitMaxs(0, 0);
            setter.visitEnd();
        }

        init.visitInsn(RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        { // toString, equals, hashCode
            List<String> names = new ArrayList<>();
            List<Handle> handles = new ArrayList<>();
            List<AttributeInfo> arrays = new ArrayList<>();

            for (AttributeInfo attr : getAttrsSorted(iface)) {
                if (attr.getter.getReturnType().isArray()) {
                    arrays.add(attr);
                    continue;
                }
                names.add(attr.fieldName());
                handles.add(new Handle(
                    H_GETFIELD,
                    typeName.getInternalName(),
                    attr.fieldName(),
                    Type.getReturnType(attr.getter).getDescriptor(),
                    false
                ));
            }

            List<Object> args = new ArrayList<>(2 + handles.size());
            args.add(typeName);
            args.add(String.join(";", names));
            args.addAll(handles);

            generateEquals(type, typeName, args, arrays);
            generateHashCode(type, typeName, args, arrays);
            generateToString(type, typeName, args);
        }

        type.visitEnd();

        return lookup.defineClass(type.toByteArray());
    }

    private static void generateEquals(@NotNull ClassWriter type, @NotNull Type name, @NotNull List<Object> args, @NotNull List<AttributeInfo> arrays) {
        MethodVisitor method = type.visitMethod(ACC_PUBLIC | ACC_FINAL, "equals", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)), null, null);
        method.visitCode();
        method.visitVarInsn(ALOAD, 0);
        method.visitVarInsn(ALOAD, 1);
        method.visitInvokeDynamicInsn("equals", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, name, Type.getType(Object.class)), BOOTSTRAP_HANDLE, args.toArray());

        Label fail = new Label();
        method.visitJumpInsn(IFEQ, fail);

        method.visitVarInsn(ALOAD, 1);
        method.visitTypeInsn(CHECKCAST, name.getInternalName());
        method.visitVarInsn(ASTORE, 2);

        for (AttributeInfo attr : arrays) {
            var attrType = Type.getReturnType(attr.getter);

            method.visitVarInsn(ALOAD, 0);
            method.visitFieldInsn(GETFIELD, name.getInternalName(), attr.fieldName(), attrType.getDescriptor());
            method.visitVarInsn(ALOAD, 2);
            method.visitFieldInsn(GETFIELD, name.getInternalName(), attr.fieldName(), attrType.getDescriptor());
            method.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "equals", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, attrType, attrType), false);
            method.visitJumpInsn(IFEQ, fail);
        }

        method.visitInsn(ICONST_1);
        method.visitInsn(IRETURN);

        method.visitLabel(fail);
        method.visitInsn(ICONST_0);
        method.visitInsn(IRETURN);

        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static void generateHashCode(@NotNull ClassWriter type, @NotNull Type name, @NotNull List<Object> args, @NotNull List<AttributeInfo> arrays) {
        MethodVisitor method = type.visitMethod(ACC_PUBLIC | ACC_FINAL, "hashCode", Type.getMethodDescriptor(Type.INT_TYPE), null, null);
        method.visitCode();
        method.visitVarInsn(ALOAD, 0);
        method.visitInvokeDynamicInsn("hashCode", Type.getMethodDescriptor(Type.INT_TYPE, name), BOOTSTRAP_HANDLE, args.toArray());
        method.visitVarInsn(ISTORE, 1);

        for (AttributeInfo attr : arrays) {
            var attrType = Type.getReturnType(attr.getter);

            method.visitLdcInsn(31);
            method.visitVarInsn(ILOAD, 1);
            method.visitInsn(IMUL);
            method.visitVarInsn(ALOAD, 0);
            method.visitFieldInsn(GETFIELD, name.getInternalName(), attr.fieldName(), attrType.getDescriptor());
            method.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "hashCode", Type.getMethodDescriptor(Type.INT_TYPE, attrType), false);
            method.visitInsn(IADD);
            method.visitVarInsn(ISTORE, 1);
        }

        method.visitVarInsn(ILOAD, 1);
        method.visitInsn(IRETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static void generateToString(@NotNull ClassWriter type, @NotNull Type name, @NotNull List<Object> args) {
        MethodVisitor method = type.visitMethod(ACC_PUBLIC | ACC_FINAL, "toString", Type.getMethodDescriptor(Type.getType(String.class)), null, null);
        method.visitCode();
        method.visitVarInsn(ALOAD, 0);
        method.visitInvokeDynamicInsn("toString", Type.getMethodDescriptor(Type.getType(String.class), name), BOOTSTRAP_HANDLE, args.toArray());
        method.visitInsn(ARETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static void generateCategory(
        @NotNull Type host,
        @NotNull Type self,
        @NotNull CategoryInfo category,
        @NotNull MethodHandles.Lookup lookup
    ) throws IllegalAccessException {
        var type = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        type.visit(V11, ACC_SUPER, self.getInternalName(), null, "java/lang/Object", new String[]{Type.getReturnType(category.getter).getInternalName()});
        type.visitNestHost(host.getInternalName());
        type.visitOuterClass(host.getInternalName(), null, null);
        type.visitInnerClass(self.getInternalName(), null, null, 0);

        type.visitField(ACC_FINAL | ACC_SYNTHETIC, "this$0", host.getDescriptor(), null, null).visitEnd();

        var init = type.visitMethod(0, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, host), null, null);
        init.visitCode();
        init.visitVarInsn(ALOAD, 0);
        init.visitVarInsn(ALOAD, 1);
        init.visitFieldInsn(PUTFIELD, self.getInternalName(), "this$0", host.getDescriptor());
        init.visitVarInsn(ALOAD, 0);
        init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        for (AttributeInfo attr : getAttrsSorted(category.getter.getReturnType())) {
            var attrType = Type.getReturnType(attr.getter);

            var getter = type.visitMethod(ACC_PUBLIC, attr.getter.getName(), Type.getMethodDescriptor(attr.getter), null, null);
            getter.visitCode();
            getter.visitVarInsn(ALOAD, 0);
            getter.visitFieldInsn(GETFIELD, self.getInternalName(), "this$0", host.getDescriptor());
            getter.visitFieldInsn(GETFIELD, host.getInternalName(), category.name() + '$' + attr.name(), attrType.getDescriptor());
            getter.visitInsn(attrType.getOpcode(IRETURN));
            getter.visitMaxs(0, 0);
            getter.visitEnd();

            var setter = type.visitMethod(ACC_PUBLIC, attr.setter.getName(), Type.getMethodDescriptor(attr.setter), null, null);
            setter.visitCode();
            setter.visitVarInsn(ALOAD, 0);
            setter.visitFieldInsn(GETFIELD, self.getInternalName(), "this$0", host.getDescriptor());
            setter.visitVarInsn(attrType.getOpcode(ILOAD), 1);
            setter.visitFieldInsn(PUTFIELD, host.getInternalName(), category.name() + '$' + attr.name(), attrType.getDescriptor());
            setter.visitInsn(RETURN);
            setter.visitMaxs(0, 0);
            setter.visitEnd();
        }

        type.visitEnd();

        lookup.defineClass(type.toByteArray());
    }

    @Nullable
    private static ExtraBinaryDataCallback<?> getExtraBinaryDataCallback0(@NotNull Class<?> cls) {
        Field field;
        try {
            field = cls.getField(TypeGenerator.CALLBACK_FIELD_NAME);
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
                method,
                setter,
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

    public static <T> void quicksort(@NotNull List<T> items, @NotNull Comparator<T> comparator) {
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

            int q = partition(items, left, right, comparator);
            state = quicksort(items, left, q - 1, comparator, state);
            state = quicksort(items, q + 1, right, comparator, state);
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
        private final Method getter;
        private final Method setter;
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
            @NotNull Method getter,
            @NotNull Method setter,
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
            this.getter = getter;
            this.setter = setter;
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
                    handle = lookup.findVarHandle(cls, fieldName(), getter.getReturnType());
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
        @NotNull MethodHandle constructor,
        @Nullable ExtraBinaryDataCallback<?> extraBinaryDataCallback
    ) {}
}
