package com.shade.decima.rtti.test;

import com.shade.decima.rtti.RTTI;
import com.shade.decima.rtti.TypeName;
import com.shade.decima.rtti.UntilDawn;
import com.shade.decima.rtti.data.Ref;
import com.shade.decima.rtti.data.Value;
import com.shade.decima.rtti.serde.ExtraBinaryDataHolder;
import com.shade.decima.rtti.serde.RTTIBinaryReader;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import com.shade.util.io.BinaryReader;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UntilDawnReader implements RTTIBinaryReader, Closeable {
    private final BinaryReader reader;

    private final Header header;
    private final TypeInfo[] typeInfo;
    private final int[] objectTypes;
    private final ObjectHeader[] objectHeaders;

    private final List<Ref<?>> pointers = new ArrayList<>();

    public UntilDawnReader(@NotNull BinaryReader reader) throws IOException {
        this.reader = reader;
        this.header = Header.read(reader);

        var typeInfoCount = reader.readInt();
        this.typeInfo = reader.readObjects(typeInfoCount, TypeInfo::read, TypeInfo[]::new);

        var objectTypesCount = reader.readInt();
        this.objectTypes = reader.readInts(objectTypesCount);

        var totalExplicitObjects = reader.readInt();
        this.objectHeaders = reader.readObjects(objectTypesCount, ObjectHeader::read, ObjectHeader[]::new);
    }

    @NotNull
    @Override
    public List<Object> read() throws IOException {
        List<Object> objects = new ArrayList<>(header.assetCount);

        for (int i = 0; i < objectTypes.length; i++) {
            var start = reader.position();

            var info = typeInfo[objectTypes[i]];
            var header = objectHeaders[i];
            var object = readCompound(RTTI.getType(info.name, UntilDawn.class));

            var end = reader.position();
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

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private <T> T readType(@NotNull Type type, @NotNull TypeName name) throws IOException {
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
            } else if (parameterized.getRawType() == Value.class) {
                return (T) readEnum((Class<?>) parameterized.getActualTypeArguments()[0]);
            }
        }
        throw new NotImplementedException();
    }

    @NotNull
    private <T> T readCompound(@NotNull Class<T> cls) throws IOException {
        T object = RTTI.newInstance(cls);
        for (RTTI.AttributeInfo attr : RTTI.getAttrsSorted(cls)) {
            if (!attr.serializable()) {
                continue;
            }
            Object value = readType(attr.type(), attr.typeName());
            attr.set(object, value);
        }
        if (object instanceof ExtraBinaryDataHolder holder) {
            holder.deserialize(reader);
        }
        return object;
    }

    @NotNull
    private Object readEnum(@NotNull Class<?> cls) throws IOException {
        var metadata = cls.getDeclaredAnnotation(RTTI.Serializable.class);
        if (metadata == null) {
            throw new IllegalArgumentException("Enum class '" + cls + "' is not annotated with " + RTTI.Serializable.class);
        }

        int value = switch (metadata.size()) {
            case Byte.BYTES -> reader.readByte();
            case Short.BYTES -> reader.readShort();
            case Integer.BYTES -> reader.readInt();
            default -> throw new IllegalArgumentException("Unexpected enum size: " + metadata.size());
        };

        return switch (cls) {
            case Class<?> c when Value.OfEnum.class.isAssignableFrom(c) -> Value.valueOf(uncheckedCast(cls), value);
            case Class<?> c when Value.OfEnumSet.class.isAssignableFrom(c) -> Value.setOf(uncheckedCast(cls), value);
            default -> throw new IllegalArgumentException("Unexpected type of enum: " + cls);
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> T uncheckedCast(Object object) {
        return (T) object;
    }

    @Nullable
    @SuppressWarnings("DuplicateBranchesInSwitch")
    private Object readAtom(@NotNull Class<?> cls, @NotNull String name) throws IOException {
        return switch (name) {
            // Base types
            case "bool" -> reader.readBoolean();
            case "wchar" -> (char) reader.readShort();
            case "uint8", "int8" -> reader.readByte();
            case "uint16", "int16" -> reader.readShort();
            case "uint", "int", "uint32", "int32" -> reader.readInt();
            case "uint64", "int64" -> reader.readLong();
            case "float" -> reader.readFloat();
            case "double" -> reader.readDouble();
            case "String" -> getString(reader);
            case "WString" -> getWString(reader);

            // Aliases
            case "RenderDataPriority", "MaterialType" -> reader.readShort();
            case "PhysicsCollisionFilterInfo" -> reader.readInt();
            case "Filename" -> getString(reader);

            default -> throw new IllegalArgumentException("Unknown atom type: " + name);
        };
    }

    @NotNull
    private Object readAtomContainer(@NotNull Class<?> cls, @NotNull TypeName.Parameterized name) throws IOException {
        var component = cls.componentType();
        var length = reader.readInt();

        if (component == byte.class) {
            return reader.readBytes(length);
        } else if (component == short.class) {
            return reader.readShorts(length);
        } else if (component == int.class) {
            return reader.readInts(length);
        } else {
            var array = Array.newInstance(component, length);
            for (int i = 0; i < length; i++) {
                Array.set(array, i, readType(component, name.argument()));
            }
            return array;
        }
    }

    @NotNull
    private List<?> readObjectContainer(@NotNull Type type, @NotNull TypeName.Parameterized name) throws IOException {
        var length = reader.readInt();
        var list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            list.add(readType(type, name.argument()));
        }
        return list;
    }

    @Nullable
    private Ref<?> readPointer() throws IOException {
        var kind = reader.readByte();
        var pointer = switch (kind) {
            case 0 -> {
                int index = reader.readInt();
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
    private static String getString(@NotNull BinaryReader reader) throws IOException {
        int index = reader.readInt();
        if (index == 0) {
            return null;
        }
        int length = reader.readInt();
        if (length == 0) {
            return "";
        }
        return reader.readString(length);
    }

    @Nullable
    private static String getWString(@NotNull BinaryReader reader) throws IOException {
        int index = reader.readInt();
        if (index == 0) {
            return null;
        }
        int length = reader.readInt();
        if (length == 0) {
            return "";
        }
        return reader.readString(length * 2, StandardCharsets.UTF_16LE);
    }

    private record Header(
        int pointerMapSize,
        int allocationCount,
        int vramAllocationCount,
        int assetCount
    ) {
        @NotNull
        static Header read(@NotNull BinaryReader reader) throws IOException {
            var version = reader.readString(14);
            if (!version.equals("RTTIBin<2.12> ")) {
                throw new IllegalStateException("Unsupported version: " + version);
            }
            var platform = UntilDawn.EPlatform.valueOf(reader.readByte());
            if (platform != UntilDawn.EPlatform._3) {
                throw new IllegalStateException("Unsupported platform: " + platform);
            }
            var endian = reader.readByte();
            if (endian != 0) {
                throw new IllegalStateException("Unsupported endian: " + endian);
            }
            var pointerMapSize = reader.readInt();
            var allocationCount = reader.readInt();
            var vramAllocationCount = reader.readInt();
            var assetCount = reader.readInt();
            return new Header(pointerMapSize, allocationCount, vramAllocationCount, assetCount);
        }
    }

    private record TypeInfo(@NotNull String name, @NotNull byte[] hash) {
        @NotNull
        static TypeInfo read(@NotNull BinaryReader reader) throws IOException {
            var name = reader.readString(reader.readInt());
            var hash = reader.readBytes(16);
            return new TypeInfo(name, hash);
        }
    }

    private record ObjectHeader(@NotNull byte[] hash, int size) {
        @NotNull
        static ObjectHeader read(@NotNull BinaryReader reader) throws IOException {
            var hash = reader.readBytes(16);
            var size = reader.readInt();
            return new ObjectHeader(hash, size);
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
            return "<pointer to object at " + index + ">";
        }
    }
}
