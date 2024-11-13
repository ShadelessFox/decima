package com.shade.decima.rtti;

import com.shade.decima.rtti.data.meta.Attr;
import com.shade.decima.rtti.data.meta.Category;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.objectweb.asm.*;

import java.lang.constant.ConstantDescs;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.TypeDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.runtime.ObjectMethods;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

final class RuntimeTypeGenerator {
    /** @see ObjectMethods#bootstrap(MethodHandles.Lookup, String, TypeDescriptor, Class, String, MethodHandle...) */
    private static final Handle BOOTSTRAP_HANDLE = new Handle(
        H_INVOKESTATIC,
        Type.getInternalName(ObjectMethods.class),
        "bootstrap",
        Type.getMethodDescriptor(
            Type.getType(Object.class),
            Type.getType(MethodHandles.Lookup.class),
            Type.getType(String.class),
            Type.getType(TypeDescriptor.class),
            Type.getType(Class.class),
            Type.getType(String.class),
            Type.getType(MethodHandle[].class)
        ),
        false
    );

    private final IsolatedClassLoader classLoader = new IsolatedClassLoader();

    RuntimeTypeGenerator() {
    }

    /**
     * Creates a concrete class that implements the given {@code cls} interface.
     * <p>
     * The returned class can be instantiated by calling the default public constructor.
     */
    @NotNull
    public Class<?> generate(@NotNull Class<?> cls) throws ReflectiveOperationException {
        if (!cls.isInterface()) {
            throw new IllegalArgumentException("Class is not an RTTI compound");
        }
        return generateClass(cls);
    }

    @NotNull
    private Class<?> generateClass(@NotNull Class<?> cls) throws ReflectiveOperationException {
        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        var name = Type.getType('L' + Type.getInternalName(cls) + "$POD;");
        writer.visit(V11, ACC_PUBLIC | ACC_SUPER, name.getInternalName(), null, Type.getInternalName(Object.class), new String[]{Type.getInternalName(cls)});

        var constructor = writer.visitMethod(ACC_PUBLIC, ConstantDescs.INIT_NAME, "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(Object.class), ConstantDescs.INIT_NAME, "()V", false);

        var categories = collectCategories(cls);
        var attrs = collectAttrs(cls);

        for (int i = 0; i < categories.size(); i++) {
            var category = categories.get(i);
            var categoryName = Type.getType('L' + name.getInternalName() + '$' + (i + 1) + ';');

            generateCategoryField(writer, constructor, name, categoryName, category);
            generateCategoryGetter(writer, name, category);
            generateCategoryClass(name, categoryName, category);
        }

        for (AttrInfo attr : attrs) {
            generateAttrField(writer, attr);

            if (attr.category() == null) {
                generateAttrGetter(writer, name, attr);
                generateAttrSetter(writer, name, attr);
            }
        }

        constructor.visitInsn(RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();

        { // toString, equals, hashCode
            List<Object> args = new ArrayList<>();
            List<AttrInfo> arrays = new ArrayList<>();
            prepareBootstrapArguments(name, attrs, args, arrays);

            generateEquals(writer, name, args, arrays);
            generateHashCode(writer, name, args, arrays);
            generateToString(writer, name, args);
        }

        writer.visitEnd();

        return classLoader.defineClass(name.getClassName(), writer.toByteArray());
    }

    private static void generateCategoryField(
        @NotNull ClassWriter writer,
        @NotNull MethodVisitor constructor,
        @NotNull Type className,
        @NotNull Type categoryName,
        @NotNull CategoryInfo category
    ) {
        var type = Type.getReturnType(category.getter());

        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitTypeInsn(NEW, categoryName.getInternalName());
        constructor.visitInsn(DUP);
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitMethodInsn(INVOKESPECIAL, categoryName.getInternalName(), ConstantDescs.INIT_NAME, Type.getMethodDescriptor(Type.VOID_TYPE, className), false);
        constructor.visitFieldInsn(PUTFIELD, className.getInternalName(), category.name(), type.getDescriptor());

        writer.visitField(ACC_FINAL | ACC_SYNTHETIC, category.name(), type.getDescriptor(), null, null).visitEnd();
        writer.visitNestMember(categoryName.getInternalName());
        writer.visitInnerClass(categoryName.getInternalName(), null, null, 0);
    }

    private static void generateCategoryGetter(
        @NotNull ClassWriter writer,
        @NotNull Type className,
        @NotNull CategoryInfo category
    ) {
        var type = Type.getReturnType(category.getter());
        var getter = writer.visitMethod(ACC_PUBLIC, category.getter().getName(), Type.getMethodDescriptor(category.getter()), null, null);
        getter.visitCode();
        getter.visitVarInsn(ALOAD, 0);
        getter.visitFieldInsn(GETFIELD, className.getInternalName(), category.name(), type.getDescriptor());
        getter.visitInsn(ARETURN);
        getter.visitMaxs(0, 0);
        getter.visitEnd();
    }

    private static void prepareBootstrapArguments(
        @NotNull Type className,
        @NotNull List<AttrInfo> attrs,
        @NotNull List<Object> args,
        @NotNull List<AttrInfo> arrays
    ) {
        List<String> names = new ArrayList<>();
        List<Handle> handles = new ArrayList<>();

        for (AttrInfo attr : attrs) {
            if (attr.getter().getReturnType().isArray()) {
                arrays.add(attr);
                continue;
            }
            names.add(attr.fieldName());
            handles.add(new Handle(
                H_GETFIELD,
                className.getInternalName(),
                attr.fieldName(),
                Type.getReturnType(attr.getter()).getDescriptor(),
                false
            ));
        }

        args.add(className);
        args.add(String.join(";", names));
        args.addAll(handles);
    }

    private static void generateAttrField(@NotNull ClassWriter writer, @NotNull AttrInfo attr) {
        var type = Type.getReturnType(attr.getter());
        var field = writer.visitField(ACC_PRIVATE, attr.fieldName(), type.getDescriptor(), null, null);
        field.visitEnd();
    }

    private static void generateAttrSetter(
        @NotNull ClassWriter writer,
        @NotNull Type className,
        @NotNull AttrInfo attr
    ) {
        var type = Type.getReturnType(attr.getter());
        var method = writer.visitMethod(ACC_PUBLIC, attr.setter().getName(), Type.getMethodDescriptor(attr.setter()), null, null);
        method.visitCode();
        method.visitVarInsn(ALOAD, 0);
        method.visitVarInsn(type.getOpcode(ILOAD), 1);
        method.visitFieldInsn(PUTFIELD, className.getInternalName(), attr.name(), type.getDescriptor());
        method.visitInsn(RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static void generateAttrGetter(
        @NotNull ClassWriter writer,
        @NotNull Type className,
        @NotNull AttrInfo attr
    ) {
        var type = Type.getReturnType(attr.getter());
        var method = writer.visitMethod(ACC_PUBLIC, attr.getter().getName(), Type.getMethodDescriptor(attr.getter()), null, null);
        method.visitCode();
        method.visitVarInsn(ALOAD, 0);
        method.visitFieldInsn(GETFIELD, className.getInternalName(), attr.name(), type.getDescriptor());
        method.visitInsn(type.getOpcode(IRETURN));
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private void generateCategoryClass(
        @NotNull Type hostClassName,
        @NotNull Type className,
        @NotNull CategoryInfo category
    ) throws ReflectiveOperationException {
        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        writer.visit(V11, ACC_SUPER, className.getInternalName(), null, Type.getInternalName(Object.class), new String[]{Type.getReturnType(category.getter()).getInternalName()});
        writer.visitNestHost(hostClassName.getInternalName());
        writer.visitOuterClass(hostClassName.getInternalName(), null, null);
        writer.visitInnerClass(className.getInternalName(), null, null, 0);
        writer.visitField(ACC_FINAL | ACC_SYNTHETIC, "this$0", hostClassName.getDescriptor(), null, null).visitEnd();

        generateCategoryConstructor(writer, hostClassName, className);

        for (AttrInfo attr : collectAttrs(category.getter().getReturnType())) {
            generateCategoryAttrGetter(writer, hostClassName, className, category, attr);
            generateCategoryAttrSetter(writer, hostClassName, className, category, attr);
        }

        writer.visitEnd();

        classLoader.defineClass(className.getClassName(), writer.toByteArray());
    }

    private static void generateCategoryConstructor(
        @NotNull ClassWriter writer,
        @NotNull Type hostClassName,
        @NotNull Type className
    ) {
        var method = writer.visitMethod(0, ConstantDescs.INIT_NAME, Type.getMethodDescriptor(Type.VOID_TYPE, hostClassName), null, null);
        method.visitCode();
        method.visitVarInsn(ALOAD, 0);
        method.visitVarInsn(ALOAD, 1);
        method.visitFieldInsn(PUTFIELD, className.getInternalName(), "this$0", hostClassName.getDescriptor());
        method.visitVarInsn(ALOAD, 0);
        method.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(Object.class), ConstantDescs.INIT_NAME, "()V", false);
        method.visitInsn(RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static void generateCategoryAttrGetter(
        @NotNull ClassWriter writer,
        @NotNull Type hostClassName,
        @NotNull Type className,
        @NotNull CategoryInfo category,
        @NotNull AttrInfo attr
    ) {
        var type = Type.getReturnType(attr.getter());
        var getter = writer.visitMethod(ACC_PUBLIC, attr.getter().getName(), Type.getMethodDescriptor(attr.getter()), null, null);
        getter.visitCode();
        getter.visitVarInsn(ALOAD, 0);
        getter.visitFieldInsn(GETFIELD, className.getInternalName(), "this$0", hostClassName.getDescriptor());
        getter.visitFieldInsn(GETFIELD, hostClassName.getInternalName(), category.name() + '$' + attr.name(), type.getDescriptor());
        getter.visitInsn(type.getOpcode(IRETURN));
        getter.visitMaxs(0, 0);
        getter.visitEnd();
    }

    private static void generateCategoryAttrSetter(
        @NotNull ClassWriter writer,
        @NotNull Type hostClassName,
        @NotNull Type className,
        @NotNull CategoryInfo category,
        @NotNull AttrInfo attr
    ) {
        var type = Type.getReturnType(attr.getter());
        var setter = writer.visitMethod(ACC_PUBLIC, attr.setter().getName(), Type.getMethodDescriptor(attr.setter()), null, null);
        setter.visitCode();
        setter.visitVarInsn(ALOAD, 0);
        setter.visitFieldInsn(GETFIELD, className.getInternalName(), "this$0", hostClassName.getDescriptor());
        setter.visitVarInsn(type.getOpcode(ILOAD), 1);
        setter.visitFieldInsn(PUTFIELD, hostClassName.getInternalName(), category.name() + '$' + attr.name(), type.getDescriptor());
        setter.visitInsn(RETURN);
        setter.visitMaxs(0, 0);
        setter.visitEnd();
    }

    private static void generateEquals(
        @NotNull ClassWriter writer,
        @NotNull Type name,
        @NotNull List<Object> args,
        @NotNull List<AttrInfo> arrays
    ) {
        var method = writer.visitMethod(ACC_PUBLIC | ACC_FINAL, "equals", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)), null, null);
        method.visitCode();
        method.visitVarInsn(ALOAD, 0);
        method.visitVarInsn(ALOAD, 1);
        method.visitInvokeDynamicInsn("equals", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, name, Type.getType(Object.class)), BOOTSTRAP_HANDLE, args.toArray());

        if (!arrays.isEmpty()) {
            var fail = new Label();
            method.visitJumpInsn(IFEQ, fail);

            method.visitVarInsn(ALOAD, 1);
            method.visitTypeInsn(CHECKCAST, name.getInternalName());
            method.visitVarInsn(ASTORE, 2);

            for (AttrInfo attr : arrays) {
                var type = Type.getReturnType(attr.getter());

                method.visitVarInsn(ALOAD, 0);
                method.visitFieldInsn(GETFIELD, name.getInternalName(), attr.fieldName(), type.getDescriptor());
                method.visitVarInsn(ALOAD, 2);
                method.visitFieldInsn(GETFIELD, name.getInternalName(), attr.fieldName(), type.getDescriptor());
                method.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Arrays.class), "equals", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, type, type), false);
                method.visitJumpInsn(IFEQ, fail);
            }

            method.visitInsn(ICONST_1);
            method.visitInsn(IRETURN);

            method.visitLabel(fail);
            method.visitInsn(ICONST_0);
        }

        method.visitInsn(IRETURN);

        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static void generateHashCode(
        @NotNull ClassWriter writer,
        @NotNull Type name,
        @NotNull List<Object> args,
        @NotNull List<AttrInfo> arrays
    ) {
        var method = writer.visitMethod(ACC_PUBLIC | ACC_FINAL, "hashCode", Type.getMethodDescriptor(Type.INT_TYPE), null, null);
        method.visitCode();
        method.visitVarInsn(ALOAD, 0);
        method.visitInvokeDynamicInsn("hashCode", Type.getMethodDescriptor(Type.INT_TYPE, name), BOOTSTRAP_HANDLE, args.toArray());

        if (!arrays.isEmpty()) {
            method.visitVarInsn(ISTORE, 1);

            for (AttrInfo attr : arrays) {
                var type = Type.getReturnType(attr.getter());

                method.visitLdcInsn(31);
                method.visitVarInsn(ILOAD, 1);
                method.visitInsn(IMUL);
                method.visitVarInsn(ALOAD, 0);
                method.visitFieldInsn(GETFIELD, name.getInternalName(), attr.fieldName(), type.getDescriptor());
                method.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "hashCode", Type.getMethodDescriptor(Type.INT_TYPE, type), false);
                method.visitInsn(IADD);
                method.visitVarInsn(ISTORE, 1);
            }

            method.visitVarInsn(ILOAD, 1);
        }

        method.visitInsn(IRETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static void generateToString(
        @NotNull ClassWriter type,
        @NotNull Type name,
        @NotNull List<Object> args
    ) {
        var method = type.visitMethod(ACC_PUBLIC | ACC_FINAL, "toString", Type.getMethodDescriptor(Type.getType(String.class)), null, null);
        method.visitCode();
        method.visitVarInsn(ALOAD, 0);
        method.visitInvokeDynamicInsn("toString", Type.getMethodDescriptor(Type.getType(String.class), name), BOOTSTRAP_HANDLE, args.toArray());
        method.visitInsn(ARETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    @NotNull
    private static List<CategoryInfo> collectCategories(@NotNull Class<?> cls) {
        List<CategoryInfo> categories = new ArrayList<>();
        for (Method method : cls.getMethods()) {
            if (!Modifier.isAbstract(method.getModifiers())) {
                // We'll look for the overloaded version of it
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
    private static List<AttrInfo> collectAttrs(@NotNull Class<?> cls) throws ReflectiveOperationException {
        List<AttrInfo> attrs = new ArrayList<>();
        collectAttrs(cls, attrs);
        return attrs;
    }

    private static void collectAttrs(
        @NotNull Class<?> cls,
        @NotNull List<AttrInfo> output
    ) throws ReflectiveOperationException {
        for (Class<?> base : cls.getInterfaces()) {
            collectAttrs(base, output);
        }
        output.addAll(collectDeclaredAttrs(cls));
    }

    @NotNull
    private static List<AttrInfo> collectDeclaredAttrs(@NotNull Class<?> cls) throws ReflectiveOperationException {
        List<AttrInfo> attrs = new ArrayList<>();
        for (Method method : cls.getDeclaredMethods()) {
            if (!Modifier.isAbstract(method.getModifiers())) {
                // We'll look for the overloaded version of it
                continue;
            }
            Category category = method.getDeclaredAnnotation(Category.class);
            if (category != null) {
                collectCategoryAttrs(new CategoryInfo(category.name(), method.getReturnType(), method), attrs);
            } else {
                collectAttr(null, method, attrs);
            }
        }
        // Position is relative to the enclosing compound type, even inside categories.
        attrs.sort(Comparator.comparingInt(AttrInfo::position));
        return attrs;
    }

    private static void collectCategoryAttrs(
        @NotNull CategoryInfo category,
        @NotNull List<AttrInfo> output
    ) throws ReflectiveOperationException {
        for (Method method : category.type().getDeclaredMethods()) {
            collectAttr(category, method, output);
        }
    }

    private static void collectAttr(
        @Nullable CategoryInfo category,
        @NotNull Method method,
        @NotNull List<AttrInfo> output
    ) throws ReflectiveOperationException {
        Attr attr = method.getDeclaredAnnotation(Attr.class);
        if (attr == null && method.getReturnType() != void.class) {
            throw new IllegalArgumentException("Unexpected method: " + method);
        }
        if (attr != null) {
            output.add(new AttrInfo(
                attr.name(),
                category,
                method,
                method.getDeclaringClass().getDeclaredMethod(method.getName(), method.getReturnType()),
                attr.position()
            ));
        }
    }

    private record CategoryInfo(
        @NotNull String name,
        @NotNull Class<?> type,
        @NotNull Method getter
    ) {}

    private record AttrInfo(
        @NotNull String name,
        @Nullable CategoryInfo category,
        @NotNull Method getter,
        @NotNull Method setter,
        int position
    ) {
        @NotNull
        public String fieldName() {
            if (category != null) {
                return category.name + '$' + name;
            } else {
                return name;
            }
        }
    }

    private static class IsolatedClassLoader extends ClassLoader {
        Class<?> defineClass(@NotNull String name, @NotNull byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }
}
