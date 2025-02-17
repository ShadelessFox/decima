package com.shade.decima.rtti.factory;

import com.shade.decima.rtti.*;
import com.shade.decima.rtti.data.Ref;
import com.shade.decima.rtti.data.Value;
import com.shade.decima.rtti.runtime.*;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.math.BigInteger;
import java.util.*;

public abstract class AbstractTypeFactory implements TypeFactory {
    private static final MethodHandle ARRAY_GETTER;
    private static final MethodHandle ARRAY_SETTER;
    private static final MethodHandle ARRAY_LENGTH;
    private static final MethodHandle LIST_GETTER;
    private static final MethodHandle LIST_SETTER;
    private static final MethodHandle LIST_LENGTH;

    private final Class<?> namespace;
    private final RuntimeTypeGenerator generator;

    private final Map<TypeName, FutureRef> pending = new HashMap<>();
    private final Map<TypeName, TypeInfo> typeByName = new HashMap<>();
    private final Map<TypeId, TypeInfo> typeById = new HashMap<>();

    static {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        try {
            ARRAY_GETTER = lookup.findStatic(Array.class, "get", MethodType.methodType(Object.class, Object.class, int.class));
            ARRAY_SETTER = lookup.findStatic(Array.class, "set", MethodType.methodType(void.class, Object.class, int.class, Object.class));
            ARRAY_LENGTH = lookup.findStatic(Array.class, "getLength", MethodType.methodType(int.class, Object.class));
            LIST_GETTER = lookup.findVirtual(List.class, "get", MethodType.methodType(Object.class, int.class));
            LIST_SETTER = lookup.findVirtual(List.class, "set", MethodType.methodType(Object.class, int.class, Object.class));
            LIST_LENGTH = lookup.findVirtual(List.class, "size", MethodType.methodType(int.class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    protected AbstractTypeFactory(@NotNull Class<?> namespace, @NotNull MethodHandles.Lookup lookup) {
        try {
            this.namespace = namespace;
            this.generator = new RuntimeTypeGenerator(MethodHandles.privateLookupIn(namespace, lookup));

            for (Class<?> cls : namespace.getDeclaredClasses()) {
                if (cls.isInterface()) {
                    var name = TypeName.of(cls.getSimpleName());
                    var info = lookup(name, cls).get();
                    var id = computeTypeId(info);
                    typeById.put(id, info);
                }
            }
            if (!pending.isEmpty()) {
                throw new IllegalStateException("Some types left unresolved: " + pending);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to initialize factory", e);
        }
    }

    @NotNull
    @Override
    public ClassTypeInfo get(@NotNull TypeId id) {
        var info = typeById.get(id);
        if (info == null) {
            throw new TypeNotFoundException("Unknown type: " + id);
        }
        return (ClassTypeInfo) info;
    }

    @NotNull
    @Override
    public ClassTypeInfo get(@NotNull Class<?> cls) {
        TypeInfo info = typeByName.get(TypeName.of(cls.getSimpleName()));
        if (info == null) {
            throw new TypeNotFoundException("Unknown type: " + cls);
        }
        if (cls.isAssignableFrom(info.type())) {
            return (ClassTypeInfo) info;
        }
        throw new IllegalArgumentException("Invalid RTTI compound: " + cls);
    }

    @NotNull
    protected TypeInfoRef lookup(
        @NotNull TypeName name,
        @NotNull Type type
    ) throws ReflectiveOperationException {
        if (pending.containsKey(name)) {
            return pending.get(name);
        }

        var info = typeByName.get(name);
        if (info != null) {
            return new ResolvedRef(info);
        }

        pending.put(name, new FutureRef(name));

        var result = switch (type) {
            // @formatter:off
            case Class<?> cls when cls == String.class || cls == BigInteger.class ->
                createAtomInfo((TypeName.Simple) name, cls);
            case Class<?> cls when cls.isPrimitive() ->
                createAtomInfo((TypeName.Simple) name, cls);
            case Class<?> cls when cls.isInterface() ->
                createClassInfo((TypeName.Simple) name, cls);
            case Class<?> cls when cls.isEnum() ->
                createEnumInfo((TypeName.Simple) name, cls);
            case Class<?> cls when cls.isArray() ->
                createArrayContainerInfo((TypeName.Parameterized) name, cls, cls.componentType());
            case ParameterizedType p when p.getRawType() == List.class ->
                createListContainerInfo((TypeName.Parameterized) name, (Class<?>) p.getRawType(), p.getActualTypeArguments()[0]);
            case ParameterizedType p when p.getRawType() == Ref.class ->
                createPointerInfo((TypeName.Parameterized) name, (Class<?>) p.getRawType(), p.getActualTypeArguments()[0]);
            case ParameterizedType p when p.getRawType() == Value.class ->
                createEnumInfo((TypeName.Simple) name, (Class<?>) p.getActualTypeArguments()[0]);
            case ParameterizedType p when p.getRawType() == Set.class
                && p.getActualTypeArguments()[0] instanceof ParameterizedType p1 && p1.getRawType() == Value.class ->
                createEnumInfo((TypeName.Simple) name, (Class<?>) p1.getActualTypeArguments()[0]);
            // @formatter:on
            default -> throw new IllegalArgumentException("Unexpected type: " + name);
        };

        return resolve(result);
    }

    @NotNull
    private static AtomTypeInfo createAtomInfo(@NotNull TypeName.Simple name, @NotNull Class<?> type) {
        return new AtomTypeInfo(name, type);
    }

    @NotNull
    private ContainerTypeInfo createArrayContainerInfo(
        @NotNull TypeName.Parameterized name,
        @NotNull Class<?> rawType,
        @NotNull Type itemType
    ) throws ReflectiveOperationException {
        return new ContainerTypeInfo(name, rawType, lookup(name.argument(), itemType), ARRAY_GETTER, ARRAY_SETTER, ARRAY_LENGTH);
    }

    @NotNull
    private ContainerTypeInfo createListContainerInfo(
        @NotNull TypeName.Parameterized name,
        @NotNull Class<?> rawType,
        @NotNull Type itemType
    ) throws ReflectiveOperationException {
        return new ContainerTypeInfo(name, rawType, lookup(name.argument(), itemType), LIST_GETTER, LIST_SETTER, LIST_LENGTH);
    }

    @NotNull
    private PointerTypeInfo createPointerInfo(
        @NotNull TypeName.Parameterized name,
        @NotNull Class<?> rawType,
        @NotNull Type itemType
    ) throws ReflectiveOperationException {
        return new PointerTypeInfo(name, rawType, lookup(name.argument(), itemType));
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private EnumTypeInfo createEnumInfo(@NotNull TypeName.Simple name, @NotNull Class<?> type) {
        Serializable serializable = type.getDeclaredAnnotation(Serializable.class);
        if (serializable == null) {
            throw new IllegalArgumentException("Enum class '" + type + "' is not annotated with " + Serializable.class);
        }
        return new EnumTypeInfo(
            name,
            (Class<? extends Enum<?>>) type,
            serializable.size(),
            Value.OfEnumSet.class.isAssignableFrom(type)
        );
    }

    @NotNull
    private TypeInfoRef resolve(@NotNull TypeInfo info) {
        FutureRef ref = pending.remove(info.name());
        if (ref == null) {
            throw new IllegalStateException("Type was not present in the queue: " + info.name());
        }
        if (typeByName.put(info.name(), info) != null) {
            throw new IllegalStateException("Type was already resolved: " + info.name());
        }
        ref.info = info;
        return ref;
    }

    @NotNull
    private ClassTypeInfo createClassInfo(
        @NotNull TypeName.Simple name,
        @NotNull Class<?> cls
    ) throws ReflectiveOperationException {
        var lookup = generator.generate(cls);
        var serializableAttrs = collectSerializableAttrs(cls, lookup);
        var displayableAttrs = new ArrayList<>(serializableAttrs);

        if (cls.getEnclosingClass() != namespace) {
            displayableAttrs.addAll(collectDeclaredAttrs(cls, lookup));
        } else {
            displayableAttrs.addAll(collectExtensionAttrs(cls, lookup));
        }

        var info = new ClassTypeInfo(
            name,
            cls,
            lookup.lookupClass(),
            collectBases(cls),
            displayableAttrs,
            serializableAttrs
        );

        generator.bind(lookup, info);
        return info;
    }

    @NotNull
    private List<ClassBaseInfo> collectBases(@NotNull Class<?> cls) throws ReflectiveOperationException {
        List<ClassBaseInfo> bases = new ArrayList<>();
        for (AnnotatedType type : cls.getAnnotatedInterfaces()) {
            Base base = type.getDeclaredAnnotation(Base.class);
            if (base != null) {
                var baseType = (Class<?>) type.getType();
                var baseTypeName = TypeName.of(baseType.getSimpleName());
                var baseTypeInfo = lookup(baseTypeName, baseType);
                bases.add(new ClassBaseInfo(baseTypeInfo, base.offset()));
            }
        }
        return List.copyOf(bases);
    }

    @NotNull
    private List<ClassAttrInfo> collectSerializableAttrs(
        @NotNull Class<?> cls,
        @NotNull MethodHandles.Lookup lookup
    ) throws ReflectiveOperationException {
        List<OrderedAttr> attrs = new ArrayList<>();
        collectSerializableAttrs(cls, lookup, attrs, 0);
        filterSerializableAttrs(attrs);
        sortSerializableAttrs(attrs);
        return attrs.stream()
            .map(OrderedAttr::info)
            .toList();
    }

    private void collectSerializableAttrs(
        @NotNull Class<?> cls,
        @NotNull MethodHandles.Lookup lookup,
        @NotNull List<OrderedAttr> output,
        int offset
    ) throws ReflectiveOperationException {
        for (AnnotatedType type : cls.getAnnotatedInterfaces()) {
            Base base = type.getDeclaredAnnotation(Base.class);
            if (base != null) {
                collectSerializableAttrs((Class<?>) type.getType(), lookup, output, offset + base.offset());
            } else {
                collectSerializableAttrs((Class<?>) type.getType(), lookup, output, offset);
            }
        }
        collectDeclaredAttrs(cls, lookup, output, offset);
    }

    @NotNull
    private List<ClassAttrInfo> collectExtensionAttrs(
        @NotNull Class<?> cls,
        @NotNull MethodHandles.Lookup lookup
    ) throws ReflectiveOperationException {
        List<OrderedAttr> attrs = new ArrayList<>();
        collectExtensionAttrs(cls, lookup, attrs);
        return attrs.stream()
            .map(OrderedAttr::info)
            .toList();
    }

    @NotNull
    private List<ClassAttrInfo> collectDeclaredAttrs(
        @NotNull Class<?> cls,
        @NotNull MethodHandles.Lookup lookup
    ) throws ReflectiveOperationException {
        List<OrderedAttr> attrs = new ArrayList<>();
        collectDeclaredAttrs(cls, lookup, attrs, 0);
        return attrs.stream()
            .map(OrderedAttr::info)
            .toList();
    }

    private void collectDeclaredAttrs(
        @NotNull Class<?> cls,
        @NotNull MethodHandles.Lookup lookup,
        @NotNull List<OrderedAttr> output,
        int offset
    ) throws ReflectiveOperationException {
        var serializable = cls.isAnnotationPresent(Serializable.class);
        var start = output.size();
        for (Method method : cls.getDeclaredMethods()) {
            if (!Modifier.isAbstract(method.getModifiers())) {
                // We'll look for the overloaded version of it
                continue;
            }
            if (method.getDeclaringClass() == TypedObject.class) {
                // We're not interested in methods from that class
                continue;
            }
            Category category = method.getDeclaredAnnotation(Category.class);
            if (category != null) {
                collectCategoryAttrs(method.getReturnType(), category.name(), lookup, output, offset, serializable);
            } else {
                collectAttr(method, null, offset, serializable, lookup, output);
            }
        }
        output
            .subList(start, output.size())
            .sort(Comparator.comparingInt(OrderedAttr::position));
    }

    private void collectExtensionAttrs(
        @NotNull Class<?> cls,
        @NotNull MethodHandles.Lookup lookup,
        @NotNull List<OrderedAttr> output
    ) throws ReflectiveOperationException {
        for (AnnotatedType type : cls.getAnnotatedInterfaces()) {
            if (type.isAnnotationPresent(Extension.class)) {
                collectDeclaredAttrs((Class<?>) type.getType(), lookup, output, 0);
            }
        }
    }

    private void collectCategoryAttrs(
        @NotNull Class<?> cls,
        @NotNull String name,
        @NotNull MethodHandles.Lookup lookup,
        @NotNull List<OrderedAttr> output,
        int offset,
        boolean serializable
    ) throws ReflectiveOperationException {
        for (Method method : cls.getDeclaredMethods()) {
            collectAttr(method, name, offset, serializable, lookup, output);
        }
    }

    private void collectAttr(
        @NotNull Method method,
        @Nullable String category,
        int offset,
        boolean serializable,
        @NotNull MethodHandles.Lookup lookup,
        @NotNull List<OrderedAttr> output
    ) throws ReflectiveOperationException {
        Attr attr = method.getDeclaredAnnotation(Attr.class);
        if (attr == null && method.getReturnType() != void.class) {
            throw new IllegalArgumentException("Unexpected method: " + method);
        }
        if (attr != null) {
            String fieldName;
            if (category != null) {
                fieldName = category + '$' + attr.name();
            } else {
                fieldName = attr.name();
            }
            ClassAttrInfo info = new ClassAttrInfo(
                attr.name(),
                category,
                lookup(TypeName.parse(attr.type()), method.getGenericReturnType()),
                lookup.findVarHandle(lookup.lookupClass(), fieldName, method.getReturnType()),
                attr.offset(),
                attr.flags()
            );
            output.add(new OrderedAttr(info, attr.position(), attr.offset() + offset, serializable));
        }
    }

    @NotNull
    protected abstract TypeId computeTypeId(@NotNull TypeInfo info);

    protected abstract void sortSerializableAttrs(@NotNull List<OrderedAttr> attrs);

    protected abstract void filterSerializableAttrs(@NotNull List<OrderedAttr> attrs);

    protected record OrderedAttr(@NotNull ClassAttrInfo info, int position, int offset, boolean serializable) {
    }

    private static class FutureRef implements TypeInfoRef {
        private final TypeName name;
        private TypeInfo info;

        public FutureRef(@NotNull TypeName name) {
            this.name = name;
        }

        @NotNull
        @Override
        public TypeName name() {
            return name;
        }

        @NotNull
        @Override
        public TypeInfo get() {
            if (info == null) {
                throw new IllegalStateException("Type '" + name + "' is not resolved");
            }
            return info;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof TypeInfoRef other && name.equals(other.name());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }

        @Override
        public String toString() {
            return info != null ? info.toString() : "<pending>";
        }
    }

    private record ResolvedRef(@NotNull TypeInfo info) implements TypeInfoRef {
        @NotNull
        @Override
        public TypeName name() {
            return info.name();
        }

        @NotNull
        @Override
        public TypeInfo get() {
            return info;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof TypeInfoRef other && info.name().equals(other.name());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(info.name());
        }

        @Override
        public String toString() {
            return info.toString();
        }
    }
}
