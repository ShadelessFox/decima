package com.shade.decima.rtti.test;

import com.shade.decima.rtti.RTTI;
import com.shade.decima.rtti.Ref;
import com.shade.decima.rtti.TypeName;
import com.shade.decima.rtti.UntilDawn;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.decima.rtti.serde.RTTIBinaryReader;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

public class UntilDawnReader implements RTTIBinaryReader {
    private static final Logger log = getLogger(UntilDawnReader.class);

    private final ByteBuffer buffer;

    private final Header header;
    private final TypeInfo[] typeInfo;
    private final int[] objectTypes;
    private final ObjectHeader[] objectHeaders;

    private final List<Ref<?>> pointers = new ArrayList<>();

    public UntilDawnReader(@NotNull ByteBuffer buffer) {
        this.buffer = buffer;
        this.header = Header.read(buffer);

        var typeInfoCount = buffer.getInt();
        this.typeInfo = BufferUtils.getObjects(buffer, typeInfoCount, TypeInfo[]::new, TypeInfo::read);

        var objectTypesCount = buffer.getInt();
        this.objectTypes = BufferUtils.getInts(buffer, objectTypesCount);

        var totalExplicitObjects = buffer.getInt();
        this.objectHeaders = BufferUtils.getObjects(buffer, objectTypesCount, ObjectHeader[]::new, ObjectHeader::read);
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public List<Object> read() {
        List<Object> objects = new ArrayList<>(header.assetCount);

        for (int i = 0; i < objectTypes.length; i++) {
            var info = typeInfo[objectTypes[i]];
            var header = objectHeaders[i];

            log.info("[{}] Reading object '{}' at offset {}", i, info.name, buffer.position());

            int start = buffer.position();

            var type = RTTI.getType(info.name, UntilDawn.class);
            var object = readCompound(type);

            var callback = (ExtraBinaryDataCallback<Object>) RTTI.getExtraBinaryDataCallback(type);
            if (callback != null) {
                callback.deserialize(buffer, object);
            }

            int end = buffer.position();

            if (header.size > 0 && end - start != header.size) {
                throw new IllegalStateException("Size mismatch for " + info.name + ": " + (end - start) + " != " + header.size);
            }

            objects.add(object);
        }

        for (Ref<?> pointer : pointers) {
            if (pointer instanceof LocalRef<?> localRef) {
                localRef.object = objects.get(localRef.index);
            }
        }

        pointers.clear();

        return objects;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private <T> T readType(@NotNull Type type, @NotNull TypeName name) {
        if (type instanceof Class<?> cls) {
            if (cls.isPrimitive() || cls == String.class) {
                return (T) readAtom(cls, name.fullName());
            } else if (cls.isArray()) {
                return (T) readAtomContainer(cls, (TypeName.Parameterized) name);
            } else if (cls.isEnum()) {
                return (T) readEnum(cls);
            } else {
                return (T) readCompound(cls);
            }
        } else if (type instanceof ParameterizedType parameterized) {
            Type argument = parameterized.getActualTypeArguments()[0];
            if (parameterized.getRawType() == List.class) {
                return (T) readObjectContainer(argument, (TypeName.Parameterized) name);
            } else if (parameterized.getRawType() == Ref.class) {
                return (T) readPointer();
            }
        }
        throw new NotImplementedException();
    }

    @NotNull
    private <T> T readCompound(@NotNull Class<T> cls) {
        T object = RTTI.newInstance(cls);
        for (RTTI.AttributeInfo attr : RTTI.getAttrsSorted(cls)) {
            if (!attr.serializable()) {
                continue;
            }
            Object value = readType(attr.type(), attr.typeName());
            attr.set(object, value);
        }
        return object;
    }

    @Nullable
    private Object readEnum(@NotNull Class<?> cls) {
        if (!RTTI.ValueEnum.class.isAssignableFrom(cls)) {
            throw new IllegalArgumentException("Enum class '" + cls + "' does not implement " + RTTI.ValueEnum.class);
        }

        var parent = (ParameterizedType) cls.getGenericInterfaces()[0];
        var type = (Class<?>) parent.getActualTypeArguments()[0];

        Number value;
        if (type == Byte.class) {
            value = buffer.get();
        } else if (type == Short.class) {
            value = buffer.getShort();
        } else if (type == Integer.class) {
            value = buffer.getInt();
        } else {
            throw new IllegalArgumentException("Unsupported enum type: " + type);
        }

        if (RTTI.ValueSetEnum.class.isAssignableFrom(cls)) {
            return RTTI.ValueSetEnum.setOf(uncheckedCast(cls), value);
        } else {
            var constant = RTTI.ValueEnum.valueOf(uncheckedCast(cls), value);
            if (constant == null) {
                log.warn("Unknown enum value {} for {}", value, cls.getSimpleName());
            }
            return constant;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T uncheckedCast(Object object) {
        return (T) object;
    }

    @Nullable
    private Object readAtom(@NotNull Class<?> cls, @NotNull String name) {
        return switch (name) {
            case "bool" -> BufferUtils.getByteBoolean(buffer);
            case "uint16" -> buffer.getShort();
            case "int", "uint32" -> buffer.getInt();
            case "uint64" -> buffer.getLong();
            case "float" -> buffer.getFloat();
            case "String", "Filename" -> getString(buffer);
            case "WString" -> getWString(buffer);
            default -> throw new IllegalArgumentException("Unknown atom type: " + name);
        };
    }

    @NotNull
    private Object readAtomContainer(@NotNull Class<?> cls, @NotNull TypeName.Parameterized name) {
        var component = cls.componentType();
        var length = buffer.getInt();

        if (component == byte.class) {
            return BufferUtils.getBytes(buffer, length);
        } else if (component == short.class) {
            return BufferUtils.getShorts(buffer, length);
        } else if (component == int.class) {
            return BufferUtils.getInts(buffer, length);
        } else {
            var array = Array.newInstance(component, length);
            for (int i = 0; i < length; i++) {
                Array.set(array, i, readType(component, name.argument()));
            }
            return array;
        }
    }

    @NotNull
    private List<?> readObjectContainer(@NotNull Type type, @NotNull TypeName.Parameterized name) {
        var length = buffer.getInt();
        var list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            list.add(readType(type, name.argument()));
        }
        return list;
    }

    @Nullable
    private Ref<?> readPointer() {
        var kind = buffer.get();
        var pointer = switch (kind) {
            case 0 -> {
                int index = buffer.getInt();
                if (index == 0) {
                    yield null;
                } else {
                    yield new LocalRef<>(index - 1);
                }
            }
            case 1 -> throw new IllegalStateException("External links are not supported");
            case 2 -> throw new IllegalStateException("Dependent links are not supported");
            default -> throw new NotImplementedException();
        };
        pointers.add(pointer);
        return pointer;
    }

    @Nullable
    private static String getString(@NotNull ByteBuffer buffer) {
        int index = buffer.getInt();
        if (index == 0) {
            return null;
        }
        int length = buffer.getInt();
        if (length == 0) {
            return "";
        }
        return new String(BufferUtils.getBytes(buffer, length), StandardCharsets.UTF_8);
    }

    @Nullable
    private static String getWString(@NotNull ByteBuffer buffer) {
        int index = buffer.getInt();
        if (index == 0) {
            return null;
        }
        int length = buffer.getInt();
        if (length == 0) {
            return "";
        }
        return new String(BufferUtils.getBytes(buffer, length * 2), StandardCharsets.UTF_16LE);
    }

    private record Header(
        int pointerMapSize,
        int allocationCount,
        int vramAllocationCount,
        int assetCount
    ) {
        @NotNull
        static Header read(@NotNull ByteBuffer buffer) {
            var magic = BufferUtils.getString(buffer, 14);
            if (!magic.equals("RTTIBin<2.12> ")) {
                throw new IllegalStateException("Invalid magic: " + magic);
            }
            var version = buffer.get();
            if (version != 3) {
                throw new IllegalStateException("Unsupported version: " + version);
            }
            var endian = buffer.get();
            if (endian != 0) {
                throw new IllegalStateException("Unsupported endian " + endian);
            }
            var pointerMapSize = buffer.getInt();
            var allocationCount = buffer.getInt();
            var vramAllocationCount = buffer.getInt();
            var assetCount = buffer.getInt();
            return new Header(pointerMapSize, allocationCount, vramAllocationCount, assetCount);
        }
    }

    private record TypeInfo(@NotNull String name, @NotNull UUID uuid) {
        @NotNull
        static TypeInfo read(@NotNull ByteBuffer buffer) {
            var name = BufferUtils.getString(buffer, buffer.getInt());
            var uuid = BufferUtils.getUUID(buffer);
            return new TypeInfo(name, uuid);
        }
    }

    private record ObjectHeader(@NotNull UUID uuid, int size) {
        @NotNull
        static ObjectHeader read(@NotNull ByteBuffer buffer) {
            var uuid = BufferUtils.getUUID(buffer);
            var size = buffer.getInt();
            return new ObjectHeader(uuid, size);
        }
    }

    private static final class LocalRef<T> implements Ref<T> {
        private final int index;
        private Object object;

        private LocalRef(int index) {
            this.index = index;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get() {
            return (T) Objects.requireNonNull(object);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (LocalRef<?>) obj;
            return Objects.equals(obj, that.object);
        }

        @Override
        public int hashCode() {
            return Objects.hash(object);
        }

        @Override
        public String toString() {
            return String.valueOf(object);
        }
    }
}
