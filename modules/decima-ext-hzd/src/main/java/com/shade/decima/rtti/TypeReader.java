package com.shade.decima.rtti;

import com.shade.decima.model.rtti.HZD;
import com.shade.decima.model.rtti.gen.RTTI;
import com.shade.decima.model.rtti.gen.Ref;
import com.shade.decima.model.util.hash.CRC32C;
import com.shade.platform.model.util.BufferUtils;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.lang.runtime.ObjectMethods;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TypeReader {
    private final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private final Map<Type, MethodHandle> objectReaders = new HashMap<>();
    private final Map<String, MethodHandle> primitiveReaders = new HashMap<>();

    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T read(@NotNull Type type, @NotNull ByteBuffer buffer) {
        try {
            return (T) getReader(type).invoke(buffer);
        } catch (Throwable e) {
            return IOUtils.sneakyThrow(e);
        }
    }

    @NotNull
    private MethodHandle getReader(@NotNull Type type) {
        return objectReaders.computeIfAbsent(type, t -> {
            final MethodHandle reader;
            try {
                reader = createReader(t);
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to create reader for " + t.getTypeName(), e);
            }
            if (reader == null) {
                throw new IllegalArgumentException("Unsupported type: " + t.getTypeName());
            }
            return reader;
        });
    }

    @NotNull
    private MethodHandle getPrimitiveReader(@NotNull String name) {
        return primitiveReaders.computeIfAbsent(name, n -> {
            final MethodHandle reader;
            try {
                reader = createPrimitiveReader(n);
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to create reader for " + name, e);
            }
            if (reader == null) {
                throw new IllegalArgumentException("Unsupported type: " + name);
            }
            return reader;
        });
    }

    @Nullable
    private MethodHandle createPrimitiveReader(@NotNull String name) throws Throwable {
        return switch (name) {
            // @formatter:off
            case "uint8", "int8" -> lookup.findVirtual(ByteBuffer.class, "get", MethodType.methodType(byte.class));
            case "uint16", "int16" -> lookup.findVirtual(ByteBuffer.class, "getShort", MethodType.methodType(short.class));
            case "uint", "int", "uint32", "int32" -> lookup.findVirtual(ByteBuffer.class, "getInt", MethodType.methodType(int.class));
            case "uintptr", "intptr", "uint64", "int64" -> lookup.findVirtual(ByteBuffer.class, "getLong", MethodType.methodType(long.class));
            case "HalfFloat" -> lookup.findStatic(TypeReader.class, "readHalf", MethodType.methodType(float.class, ByteBuffer.class));
            case "float" -> lookup.findVirtual(ByteBuffer.class, "getFloat", MethodType.methodType(float.class));
            case "double" -> lookup.findVirtual(ByteBuffer.class, "getDouble", MethodType.methodType(double.class));
            case "bool" -> lookup.findStatic(TypeReader.class, "readBoolean", MethodType.methodType(boolean.class, ByteBuffer.class));
            case "String" -> lookup.findStatic(TypeReader.class, "readString", MethodType.methodType(String.class, ByteBuffer.class));
            default -> null;
            // @formatter:on
        };
    }

    @Nullable
    private MethodHandle createReader(@NotNull Type type) throws Throwable {
        if (type instanceof Class<?> cls) {
            if (cls.getDeclaredAnnotation(RTTI.Serializable.class) != null) {
                return lookup
                    .findVirtual(TypeReader.class, "readObject", MethodType.methodType(Object.class, Class.class, ByteBuffer.class))
                    .bindTo(this)
                    .bindTo(type);
            }
            if (RTTI.Enum.OfByte.class.isAssignableFrom(cls)) {
                return lookup
                    .findVirtual(TypeReader.class, "readByteEnum", MethodType.methodType(Enum.class, Class.class, ByteBuffer.class))
                    .bindTo(this)
                    .bindTo(type);
            }
            if (RTTI.Enum.OfShort.class.isAssignableFrom(cls)) {
                return lookup
                    .findVirtual(TypeReader.class, "readShortEnum", MethodType.methodType(Enum.class, Class.class, ByteBuffer.class))
                    .bindTo(this)
                    .bindTo(type);
            }
            if (RTTI.Enum.OfInt.class.isAssignableFrom(cls)) {
                return lookup
                    .findVirtual(TypeReader.class, "readIntEnum", MethodType.methodType(Enum.class, Class.class, ByteBuffer.class))
                    .bindTo(this)
                    .bindTo(type);
            }
        }
        if (type instanceof ParameterizedType pt && pt.getRawType() == List.class) {
            final Type argument = pt.getActualTypeArguments()[0];
            return lookup
                .findVirtual(TypeReader.class, "readArray", MethodType.methodType(List.class, Type.class, ByteBuffer.class))
                .bindTo(this)
                .bindTo(argument);
        }
        if (type instanceof ParameterizedType pt && pt.getRawType() == Ref.class) {
            return lookup
                .findVirtual(TypeReader.class, "readRef", MethodType.methodType(Ref.class, ByteBuffer.class))
                .bindTo(this);
        }
        return null;
    }

    @NotNull
    public List<?> readArray(@NotNull Type type, @NotNull ByteBuffer buffer) {
        final int size = buffer.getInt();
        final List<Object> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(read(type, buffer));
        }
        return result;
    }

    @NotNull
    public Ref<?> readRef(@NotNull ByteBuffer buffer) {
        final byte type = buffer.get();
        return switch (type) {
            case 0 -> new RefImpl<>(null, null, true);
            case 1 -> new RefImpl<>(read(HZD.GGUUID.class, buffer), null, true);
            case 2 -> new RefImpl<>(read(HZD.GGUUID.class, buffer), readString(buffer), true);
            case 5 -> new RefImpl<>(read(HZD.GGUUID.class, buffer), null, false);
            case 3 -> new RefImpl<>(read(HZD.GGUUID.class, buffer), readString(buffer), false);
            default -> throw new IllegalArgumentException("Invalid reference type: " + type);
        };
    }

    private int depth = 0;

    @NotNull
    public Object readObject(@NotNull Class<?> type, @NotNull ByteBuffer buffer) throws Throwable {
        final Map<Method, Object> values = new HashMap<>();
        depth++;
        for (RTTI.AttrWithOffset attrWithOffset : RTTI.collectAttrs(type)) {
            final Method method = attrWithOffset.method();
            final RTTI.Attr attr = attrWithOffset.attr();
            System.out.println("\t".repeat(depth) + "Reading " + method.getName() + " (type is " + method.getReturnType().getName() + ") of " + type.getName() + " at offset " + buffer.position());
            final MethodHandle reader;
            if (attr.type().isEmpty()) {
                reader = getReader(method.getGenericReturnType());
            } else {
                reader = getPrimitiveReader(attr.type());
            }
            final Object value = reader.invoke(buffer);
            System.out.println("\t".repeat(depth) + " - " + value);
            values.put(method, value);
        }
        depth--;
        return Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[]{type},
            new MyInvocationHandler(lookup, type, values)
        );
    }

    @NotNull
    public <T extends Enum<T> & RTTI.Enum.OfByte> T readByteEnum(@NotNull Class<T> type, @NotNull ByteBuffer buffer) {
        final byte value = buffer.get();
        for (T constant : type.getEnumConstants()) {
            if (constant.value() == value) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Invalid enum value: " + value);
    }

    @NotNull
    public <T extends Enum<T> & RTTI.Enum.OfShort> T readShortEnum(@NotNull Class<T> type, @NotNull ByteBuffer buffer) {
        final short value = buffer.getShort();
        for (T constant : type.getEnumConstants()) {
            if (constant.value() == value) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Invalid enum value: " + value);
    }

    @NotNull
    public <T extends Enum<T> & RTTI.Enum.OfInt> T readIntEnum(@NotNull Class<T> type, @NotNull ByteBuffer buffer) {
        final int value = buffer.getInt();
        for (T constant : type.getEnumConstants()) {
            if (constant.value() == value) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Invalid enum value: " + value);
    }

    @NotNull
    public static String readString(@NotNull ByteBuffer buffer) {
        final int size = buffer.getInt();
        if (size > 0) {
            final int hash = buffer.getInt();
            final byte[] data = BufferUtils.getBytes(buffer, size);
            if (hash != CRC32C.calculate(data)) {
                throw new IllegalArgumentException("Data is corrupted (mismatched checksum)");
            }
            return new String(data, StandardCharsets.UTF_8);
        } else {
            return "";
        }
    }

    public static float readHalf(@NotNull ByteBuffer buffer) {
        return BufferUtils.getHalfFloat(buffer);
    }

    public static boolean readBoolean(@NotNull ByteBuffer buffer) {
        final byte value = buffer.get();
        return switch (value) {
            case 0 -> false;
            case 1 -> true;
            default -> throw new IllegalArgumentException("Invalid boolean value: " + value);
        };
    }

    private record RefImpl<T>(@Nullable HZD.GGUUID uuid, @Nullable String path, boolean link) implements Ref<T> {
        @Override
        public T get() {
            throw new NotImplementedException();
        }
    }

    private static class MyInvocationHandler implements InvocationHandler {
        private final MethodHandles.Lookup lookup;
        private final Class<?> type;
        private final Map<Method, Object> values;
        private final Map<Method, MethodHandle> methods = new ConcurrentHashMap<>();

        private MyInvocationHandler(
            @NotNull MethodHandles.Lookup lookup,
            @NotNull Class<?> type,
            @NotNull Map<Method, Object> values
        ) {
            this.lookup = lookup;
            this.type = type;
            this.values = values;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (isObjectMethod(method)) {
                final MethodHandle mh = getObjectMethod(method);
                if (args == null) {
                    return mh.invoke(proxy);
                } else {
                    return mh.invoke(proxy, args);
                }
            }
            final Object value = values.get(method);
            if (value != null) {
                return value;
            }
            throw new IllegalStateException("Bad proxy method: " + method);
        }

        private static boolean isObjectMethod(Method m) {
            return switch (m.getName()) {
                case "toString" -> m.getReturnType() == String.class
                    && m.getParameterCount() == 0;
                case "hashCode" -> m.getReturnType() == int.class
                    && m.getParameterCount() == 0;
                case "equals" -> m.getReturnType() == boolean.class
                    && m.getParameterCount() == 1
                    && m.getParameterTypes()[0] == Object.class;
                default -> false;
            };
        }

        @NotNull
        private MethodHandle getObjectMethod(@NotNull Method method) {
            return methods.computeIfAbsent(method, m -> {
                try {
                    final List<MethodHandle> getters = new ArrayList<>();
                    final List<String> names = new ArrayList<>();

                    for (RTTI.AttrWithOffset attr : RTTI.collectAttrs(type)) {
                        final MethodHandle mh = lookup.unreflect(attr.method());
                        final MethodHandle mh1 = mh.asType(mh.type().changeParameterType(0, type));
                        getters.add(mh1);
                        names.add(attr.method().getName());
                    }

                    return switch (m.getName()) {
                        case "toString" ->
                            bootstrap(type, "toString", MethodType.methodType(String.class, type), getters, names);
                        case "hashCode" ->
                            bootstrap(type, "hashCode", MethodType.methodType(int.class, type), getters, names);
                        case "equals" ->
                            bootstrap(type, "equals", MethodType.methodType(boolean.class, type, Object.class), getters, names);
                        default -> throw new IllegalArgumentException("Unsupported method: " + m);
                    };
                } catch (Throwable e) {
                    throw new IllegalStateException("Failed to create method handle for " + m, e);
                }
            });
        }

        @NotNull
        private MethodHandle bootstrap(
            @NotNull Class<?> receiverClass,
            @NotNull String name,
            @NotNull MethodType type,
            @NotNull List<MethodHandle> getters,
            @NotNull List<String> names
        ) throws Throwable {
            final CallSite callSite = (CallSite) ObjectMethods.bootstrap(
                MethodHandles.lookup(),
                name,
                type,
                receiverClass,
                String.join(";", names),
                getters.toArray(MethodHandle[]::new)
            );
            return callSite.getTarget();
        }
    }
}
