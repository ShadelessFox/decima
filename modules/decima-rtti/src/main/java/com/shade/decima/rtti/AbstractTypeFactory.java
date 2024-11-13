package com.shade.decima.rtti;

import com.shade.decima.rtti.data.Ref;
import com.shade.decima.rtti.data.Value;
import com.shade.decima.rtti.runtime.*;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.math.BigInteger;
import java.util.*;

public abstract class AbstractTypeFactory implements TypeFactory {
    private final Map<TypeName, FutureRef> pending = new HashMap<>();
    private final Map<TypeName, TypeInfo> cache = new HashMap<>();
    private final Map<TypeId, TypeInfo> ids = new HashMap<>();

    private static class IsolatedClassLoader extends ClassLoader {
        private final MethodHandles.Lookup lookup = MethodHandles.lookup();
    }

    private final IsolatedClassLoader classLoader = new IsolatedClassLoader();

    protected AbstractTypeFactory(@NotNull Class<?> namespace) {
        try {
            for (Class<?> cls : namespace.getDeclaredClasses()) {
                if (cls.isInterface()) {
                    var name = TypeName.of(cls.getSimpleName());
                    var info = lookup(name, cls).get();
                    var id = computeTypeId(info);
                    ids.put(id, info);
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
    private static AtomTypeInfo createAtomInfo(@NotNull TypeName.Simple name, @NotNull Class<?> type) {
        return new AtomTypeInfo(name, type);
    }

    @NotNull
    @Override
    public ClassTypeInfo get(@NotNull TypeId id) {
        var info = ids.get(id);
        if (info == null) {
            throw new IllegalArgumentException("Unknown type: " + id);
        }
        return (ClassTypeInfo) info;
    }

    @NotNull
    @Override
    public ClassTypeInfo get(@NotNull Class<?> cls) {
        TypeInfo info = cache.get(TypeName.of(cls.getSimpleName()));
        if (info == null) {
            throw new IllegalArgumentException("Unknown type: " + cls);
        }
        if (info.type() instanceof Class<?> c && cls.isAssignableFrom(c)) {
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

        var info = cache.get(name);
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
                createContainerInfo((TypeName.Parameterized) name, cls, cls.componentType());
            case ParameterizedType p when p.getRawType() == List.class ->
                createContainerInfo((TypeName.Parameterized) name, (Class<?>) p.getRawType(), p.getActualTypeArguments()[0]);
            case ParameterizedType p when p.getRawType() == Ref.class ->
                createPointerInfo((TypeName.Parameterized) name, (Class<?>) p.getRawType(), p.getActualTypeArguments()[0]);
            case ParameterizedType p when p.getRawType() == Value.class ->
                createEnumInfo((TypeName.Simple) name, (Class<?>) p.getActualTypeArguments()[0]);
            // @formatter:on
            default -> throw new IllegalArgumentException("Unexpected type: " + name);
        };

        return resolve(result);
    }

    @NotNull
    private ContainerTypeInfo createContainerInfo(
        @NotNull TypeName.Parameterized name,
        @NotNull Class<?> rawType,
        @NotNull Type itemType
    ) throws ReflectiveOperationException {
        return new ContainerTypeInfo(name, rawType, lookup(name.argument(), itemType));
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
        RTTI.Serializable serializable = type.getDeclaredAnnotation(RTTI.Serializable.class);
        if (serializable == null) {
            throw new IllegalArgumentException("Enum class '" + type + "' is not annotated with " + RTTI.Serializable.class);
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
        if (cache.put(info.name(), info) != null) {
            throw new IllegalStateException("Type was already resolved: " + info.name());
        }
        ref.resolved = info;
        return ref;
    }

    @NotNull
    private ClassTypeInfo createClassInfo(
        @NotNull TypeName.Simple name,
        @NotNull Class<?> cls
    ) throws ReflectiveOperationException {
        var lookup = MethodHandles.privateLookupIn(cls, classLoader.lookup);
        var holder = RuntimeTypeGenerator.generate(cls, lookup);
        return new ClassTypeInfo(
            name,
            holder.lookupClass(),
            collectBases(cls),
            collectDeclaredAttrs(cls, holder),
            collectSerializableAttrs(cls, holder)
        );
    }

    @NotNull
    private List<ClassBaseInfo> collectBases(@NotNull Class<?> cls) throws ReflectiveOperationException {
        List<ClassBaseInfo> bases = new ArrayList<>();
        for (AnnotatedType type : cls.getAnnotatedInterfaces()) {
            RTTI.Base base = type.getDeclaredAnnotation(RTTI.Base.class);
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
        //if (cls.getSimpleName().equals(""))
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
            RTTI.Base base = type.getDeclaredAnnotation(RTTI.Base.class);
            if (base != null) {
                collectSerializableAttrs((Class<?>) type.getType(), lookup, output, offset + base.offset());
            } else {
                collectSerializableAttrs((Class<?>) type.getType(), lookup, output, offset);
            }
        }
        collectDeclaredAttrs(cls, lookup, output, offset);
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
        var serializable = cls.isAnnotationPresent(RTTI.Serializable.class);
        var start = output.size();
        for (Method method : cls.getDeclaredMethods()) {
            if (!Modifier.isAbstract(method.getModifiers())) {
                // We'll look for the overloaded version of it
                continue;
            }
            RTTI.Category category = method.getDeclaredAnnotation(RTTI.Category.class);
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
        RTTI.Attr attr = method.getDeclaredAnnotation(RTTI.Attr.class);
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

    protected static class FutureRef implements TypeInfoRef {
        private final TypeName name;
        private TypeInfo resolved;

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
            if (resolved == null) {
                throw new IllegalStateException("Type '" + name + "' is not resolved");
            }
            return resolved;
        }

        @Override
        public String toString() {
            return resolved != null ? resolved.toString() : "<pending>";
        }
    }

    protected record ResolvedRef(@NotNull TypeInfo get) implements TypeInfoRef {
        @NotNull
        @Override
        public TypeName name() {
            return get.name();
        }

        @Override
        public String toString() {
            return get.toString();
        }
    }
}
